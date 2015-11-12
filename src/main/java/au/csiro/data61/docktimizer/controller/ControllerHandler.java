package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.exception.*;
import au.csiro.data61.docktimizer.interfaces.CloudController;
import au.csiro.data61.docktimizer.interfaces.DatabaseController;
import au.csiro.data61.docktimizer.interfaces.LoadBalancerController;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VMType;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 */
public class ControllerHandler {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(ControllerHandler.class);
    private static final long VM_STARTUP_TIMEOUT = 6;
    public static final boolean OPENSTACK = false; // enable to use openstack
    public static boolean SETUP = true;

    public static boolean DEBUG_MODE = false;
    private final LoadBalancerController loadBalancerController;

    final ExecutorService vmStartingService = Executors.newFixedThreadPool(10);
    final ExecutorService containerDeployService = Executors.newFixedThreadPool(20);
    private long tau_t;


    private DatabaseController databaseController;
    private static CloudController cloudController;
    private JavaDockerController dockerController;

    private Map<VMType, List<VirtualMachine>> vmMap;
    private Map<DockerContainer, List<DockerContainer>> dockerMap;
    private static ControllerHandler instance;

    private ControllerHandler() {
        loadBalancerController = HAProxyLoadBalancerController.getInstance();
        loadBalancerController.initialize();
    }

    public static ControllerHandler getInstance() {
        if (instance == null) {
            instance = new ControllerHandler();
        }
        return instance;
    }

    public void initializeAndUpdate(long tau_t) {
        this.tau_t = tau_t;

        if (databaseController == null) {
            databaseController = MysqlDatabaseController.getInstance();
        }
        databaseController.initializeAndUpdate(tau_t);

        if (cloudController == null) {
            if (ControllerHandler.OPENSTACK) {
                cloudController = OpenstackCloudController.getInstance();
            } else
                cloudController = AWSCloudController.getInstance();

        }
        if (dockerController == null) {
            dockerController = JavaDockerController.getInstance();
            dockerController.initialize();
        }


        vmMap = databaseController.getVmMap(true);
        dockerMap = databaseController.getDockerMap();

    }


    /**
     * starts ur updates a virtual machine
     *
     * @param virtualMachines to be started or updted
     */
    public void startVirtualMachine(List<VirtualMachine> virtualMachines) {


        List<Future<VirtualMachine>> vmStartingFutures = new ArrayList<>();
        for (final VirtualMachine virtualMachine : virtualMachines) {

            //else start new VM
            Future<VirtualMachine> startingVM = vmStartingService.submit(new Callable<VirtualMachine>() {
                @Override
                public VirtualMachine call() throws Exception {

                    LOG.info("starting VM " + virtualMachine.getName() + " ...");
                    try {
                        cloudController.startInstance(virtualMachine);

                        return virtualMachine;
                    } catch (NodeStartingException | HardwareNotFoundException | UnsupportedVMNaming | ImageNotFoundException e) {
                        LOG.error("Could not start VirtualMachine", e);
                        return null;
                    }
                }
            });
            vmStartingFutures.add(startingVM);

        }
        //check IP addresses
        for (Future<VirtualMachine> startingFuture : vmStartingFutures) {
            try {
                VirtualMachine virtualMachine = startingFuture.get(VM_STARTUP_TIMEOUT, TimeUnit.MINUTES);
                if (virtualMachine == null) {
                    throw new Exception("error starting vm");
                }
            } catch (Exception e) {
                LOG.error("Could not start VM in less than 4 minutes ", e);
            }
        }


    }

    /**
     * starts ur updates a virtual machine
     *
     * @param virtualMachines to be started or updted
     */
    public void updateVirtualMachine(List<VirtualMachine> virtualMachines) {


        List<Future<VirtualMachine>> vmStartingFutures = new ArrayList<>();
        for (final VirtualMachine virtualMachine : virtualMachines) {
            //If VM running, update start date for another BTU
            LOG.info("updating VM " + virtualMachine.getName() + " ... " + virtualMachine.getId());
            LOG.info("remaining time:  " + virtualMachine.getName() + " ... " + (virtualMachine.getToBeTerminatedAt() - tau_t));

            Future<VirtualMachine> startingVM = vmStartingService.submit(new Callable<VirtualMachine>() {
                @Override
                public VirtualMachine call() throws Exception {

                    List<VirtualMachine> startOrReplace = new ArrayList<>();
                    startOrReplace.add(virtualMachine);

                    //start or replace containers
                    startOrReplaceContainer(startOrReplace);

                    return virtualMachine;
                }
            });
            vmStartingFutures.add(startingVM);

        }

        //check IP addresses
        for (Future<VirtualMachine> startingFuture : vmStartingFutures) {
            try {
                VirtualMachine virtualMachine = startingFuture.get(VM_STARTUP_TIMEOUT, TimeUnit.MINUTES);
                if (virtualMachine == null) {
                    throw new Exception("error starting vm");
                }
            } catch (Exception e) {
                LOG.error("Could not start VM in less than 4 minutes ", e);
            }
        }

    }

    public void ensureDockerContainerAreDeployed(List<VirtualMachine> virtualMachines) {
        List<Future<Boolean>> vmStartingFutures = new ArrayList<>();

        for (final VirtualMachine virtualMachine : virtualMachines) {
            Future<Boolean> startingVM = containerDeployService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    for (int i = 0; i < 5; i++) {
                        long millis = 30000L;
                        try {
                            List<DockerContainer> alreadyDeployedContainers = dockerController.getDockers(virtualMachine);
                            if (alreadyDeployedContainers.size() >= virtualMachine.getDeployedContainers().size()) {
                                //if all containers found
                                LOG.info("all found");
                                return true;
                            } else {
                                //else, sleep and try again
                                LOG.info(String.format("at vm: %s not all found (expected %s got %s ... retrying in %s",
                                        virtualMachine.getIp(),
                                        virtualMachine.getDeployedContainers().size(),
                                        alreadyDeployedContainers.size(),
                                        millis));

                                Thread.sleep(millis);
                            }
                        } catch (Exception e) {
                            //ignore
                            LOG.info("VM " + virtualMachine.getName() + "not reachable, retry in " + millis / 1000 +
                                    " seconds");
                            Thread.sleep(millis);
                        }
                    }
                    LOG.info("Could not find all containers " + virtualMachine.getIp());
                    return false;
                }
            });
            vmStartingFutures.add(startingVM);
        }
        LOG.info("waiting for deploying containers... " + vmStartingFutures.size());
        for (Future<Boolean> dockerDeployingFuture : vmStartingFutures) {
            try {
                Boolean isOK = dockerDeployingFuture.get(VM_STARTUP_TIMEOUT, TimeUnit.MINUTES);
            } catch (Exception e) {
                LOG.info("Could not get Docker containers: ", e);
            }
        }

    }

    public void updateHAProxyConfiguration(List<VirtualMachine> virtualMachines) {
        loadBalancerController.updateHAProxyConfiguration(virtualMachines);
    }

    /**
     * starts or replaces a set of containers
     *
     * @param virtualMachines a list of virtual machines including their assigned docker containers
     */
    private void startOrReplaceContainer(List<VirtualMachine> virtualMachines) {
        for (final VirtualMachine virtualMachine : virtualMachines) {

            final List<DockerContainer> containerList = virtualMachine.getDeployedContainers();
            virtualMachine.setDeplyoedContainers(new ArrayList<DockerContainer>());
            for (final DockerContainer dockerContainer : containerList) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            dockerController.startDocker(virtualMachine, dockerContainer);
                        } catch (CouldNotStartDockerException e) {
                            LOG.error("Could not start docker container", e);
                        }
                    }
                }).start();

                virtualMachine.addDockerContainer(dockerContainer);
            }

            // remove old containers which are not needed anymore,
            Map<DockerContainer, List<DockerContainer>> dockerMap = getDockerMap();
            for (final DockerContainer dockerContainer : dockerMap.keySet()) {
                if (!virtualMachine.getDeployedContainers().contains(dockerContainer)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                dockerController.stopDocker(virtualMachine, dockerContainer);
                            } catch (CouldNotStopDockerException ignore) {
                                //can be ignored, container may not have been deployed on that VM
                            }
                        }
                    }).start();
                }
            }
        }
    }

    public void terminateVM(VirtualMachine virtualMachine) {
        virtualMachine.setRunning(false);
        virtualMachine.setStartDate(0);
        cloudController.terminateInstance(virtualMachine);
        databaseController.update(virtualMachine);

    }

    public Map<VMType, List<VirtualMachine>> getVMMap(boolean update) {
        return databaseController.getVmMap(update);
    }

    public Map<DockerContainer, List<DockerContainer>> getDockerMap() {
        return databaseController.getDockerMap();
    }

    public int getInvocations(DockerContainer dockerContainerType) {
        return databaseController.getInvocations(dockerContainerType);
    }

    public void setDatabaseController(DatabaseController databaseController) {
        this.databaseController = databaseController;
    }

    public List<VirtualMachine> updateExistingServers() {

        List<VirtualMachine> serverList = new ArrayList<>();
        if (MysqlDatabaseController.BASELINE_TYPE.equalsIgnoreCase("DEFAULT")) {
            serverList = cloudController.getServerList();
            for (VirtualMachine virtualMachine : serverList) {
                try {
                    LOG.info("found VM " + virtualMachine.getName());
                    LOG.info("get dockers for this VM");
                    List<DockerContainer> newList = new ArrayList<>();
                    List<DockerContainer> dockers = dockerController.getDockers(virtualMachine);
                    for (DockerContainer docker : dockers) {
                        docker = databaseController.getDocker(docker.getDockerImage().getAppId());
                        newList.add(docker);
                    }
                    virtualMachine.setDeplyoedContainers(newList);
                    LOG.info("found dockers for this VM " + dockers.size());
                    long newStartDate = new Date().getTime();
                    virtualMachine.setStartDate(newStartDate);
                    virtualMachine.setToBeTerminatedAt(newStartDate + virtualMachine.getVmType().leasingDuration);
                    virtualMachine.setRunning(true);
                    databaseController.update(virtualMachine);
                } catch (Exception e) {
                    LOG.error("Could not update VM ", e);
                }
            }
        }
        return serverList;
    }

    public void setUpOneForEach() {
        Map<VMType, List<VirtualMachine>> vmMap = getVMMap(true);

        for (VMType vmType : vmMap.keySet()) {
            switch (vmType) {

                case M1_SMALL:
                    if (SETUP) {
                        Map<DockerContainer, List<DockerContainer>> dockerMap = instance.getDockerMap();

                        LOG.info("setting up 3 small VMs with 1 service each");

                        List<VirtualMachine> virtualMachines = vmMap.get(vmType);
                        int counter = 0;
                        List<VirtualMachine> tmpList = new ArrayList<>();

                        for (DockerContainer dockerContainer : dockerMap.keySet()) {
                            if (counter == 3) {
                                break;
                            }
                            VirtualMachine virtualMachine = virtualMachines.get(counter++);
                            virtualMachine.addDockerContainer(dockerContainer);
                            tmpList.add(virtualMachine);
                            counter++;


                        }
                        startVirtualMachine(tmpList);
                        for (VirtualMachine virtualMachine : tmpList) {
                            virtualMachine.setRunning(true);
                            virtualMachine.setStartDate(tau_t);
                            virtualMachine.setToBeTerminatedAt(tau_t + virtualMachine.getVmType().leasingDuration);
                            databaseController.update(virtualMachine);
                        }

                        ensureDockerContainerAreDeployed(tmpList);
                        updateHAProxyConfiguration(tmpList);
                        SETUP = false;
                        break;
                    }
            }
        }
    }

    public void setUpAllOnOne() {
        Map<VMType, List<VirtualMachine>> vmMap = getVMMap(true);
        for (VMType vmType : vmMap.keySet()) {
            switch (vmType) {

                case M1_LARGE:
                    if (SETUP) {

                        LOG.info("setting up 1 large VM with 3 services");
                        Map<DockerContainer, List<DockerContainer>> dockerMap = instance.getDockerMap();

                        List<VirtualMachine> virtualMachines = vmMap.get(vmType);
                        int counter = 0;
                        List<VirtualMachine> tmpList = new ArrayList<>();

                        VirtualMachine virtualMachine = virtualMachines.get(0);
                        for (DockerContainer dockerContainer : dockerMap.keySet()) {

                            if (counter == 3) {
                                break;
                            }
                            counter++;
                            virtualMachine.addDockerContainer(dockerContainer);
                        }
                        tmpList.add(virtualMachine);

                        startVirtualMachine(tmpList);
                        virtualMachine.setRunning(true);
                        virtualMachine.setStartDate(tau_t);
                        virtualMachine.setToBeTerminatedAt(tau_t + virtualMachine.getVmType().leasingDuration);
                        databaseController.update(virtualMachine);
                        SETUP = false;
                        ensureDockerContainerAreDeployed(tmpList);
                        updateHAProxyConfiguration(tmpList);
                        break;
                    }
            }
        }
    }

    public static CloudController getCloudControllerInstance() {
        if (cloudController != null) {
            return cloudController;
        }
        if (OPENSTACK) {
            cloudController = OpenstackCloudController.getInstance();
        } else
            cloudController = AWSCloudController.getInstance();

        return cloudController;
    }
}
