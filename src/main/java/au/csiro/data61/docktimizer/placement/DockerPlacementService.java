package au.csiro.data61.docktimizer.placement;

import au.csiro.data61.docktimizer.controller.ControllerHandler;
import au.csiro.data61.docktimizer.controller.MysqlDatabaseController;
import au.csiro.data61.docktimizer.interfaces.DatabaseController;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VMType;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import net.sf.javailp.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 */
public class DockerPlacementService {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(DockerPlacementService.class);
    public static final long SLEEP_TIME = 60 * 1000 * 5;
    private static boolean SETUP = true;
    private static int MAX_LOOP = 1;
    private static DockerPlacementService instance;


    private DockerPlacement dockerPlacement;
    private DatabaseController databaseController;
    private ControllerHandler controllerHandler;

    private DockerPlacementService() {
        initialize();
    }

    public synchronized static DockerPlacementService getInstance() {
        if (instance == null) {
            instance = new DockerPlacementService();
        }
        return instance;
    }

    private void initialize() {
        if (dockerPlacement == null) {
            LOG.info("First initialization");
            dockerPlacement = DockerPlacement.getInstance();
            databaseController = MysqlDatabaseController.getInstance();

            controllerHandler = ControllerHandler.getInstance();
            controllerHandler.setDatabaseController(databaseController);

            dockerPlacement.setControllerHandler(controllerHandler);

        }
    }

    public void computePlacement(long tau_t) {
        LOG.info("Compute Placement");
        Result result = dockerPlacement.solvePlacement(tau_t);

        Number objective = result.getObjective();

        print(result, objective);

        LOG.info("Update Variables");
        updateVariables(result, tau_t);


    }

    private void updateVariables(Result result, long tau_t) {
        Map<DockerContainer, List<DockerContainer>> dockerMap = databaseController.getDockerMap();
        Map<VMType, List<VirtualMachine>> vmMap = databaseController.getVmMap(false);

        Map<String, Number> xValues = new HashMap<>();
        Map<String, Number> yValues = new HashMap<>();
        final Map<VirtualMachine, List<DockerContainer>> xValueContainer = new HashMap<>();
        final List<VirtualMachine> vmsToBeStarted = new ArrayList<>();
        final List<VirtualMachine> vmsToBeUpdated = new ArrayList<>();
        final List<VirtualMachine> done = new ArrayList<>();

        for (DockerContainer dockerContainerType : dockerMap.keySet()) {
            for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                for (VMType vmType : vmMap.keySet()) {
                    for (VirtualMachine virtualMachine : vmMap.get(vmType)) {
                        String decisionVariableX = dockerPlacement.getDecisionVariableX(dockerContainer, virtualMachine);
                        Number value = result.get(decisionVariableX);
                        xValues.put(decisionVariableX, value);
                        if (value.intValue() == 1) {
                            List<DockerContainer> containerList = xValueContainer.get(virtualMachine);
                            if (containerList == null) {
                                containerList = new ArrayList<>();
                            }
                            containerList.add(dockerContainer);
                            xValueContainer.put(virtualMachine, containerList);
                        }

                        String decisionVariableY = dockerPlacement.getDecisionVariableY(virtualMachine);
                        Number yValue = result.get(decisionVariableY);
                        yValues.put(decisionVariableY, yValue);

                        if (yValue.intValue() == 1 && !vmsToBeStarted.contains(virtualMachine) && !virtualMachine.isRunning()) {
                            vmsToBeStarted.add(virtualMachine);
                        } else if (yValue.intValue() == 1 && !vmsToBeUpdated.contains(virtualMachine) && virtualMachine.isRunning()) {
                            vmsToBeUpdated.add(virtualMachine);
                        }
                    }
                }
            }
        }

        List<VirtualMachine> vmStarted = new ArrayList<>();
        List<VirtualMachine> vmsUpdated = new ArrayList<>();
        List<VirtualMachine> justUpdateDocker = new ArrayList<>();
        for (final VirtualMachine virtualMachine : xValueContainer.keySet()) {
            if (vmsToBeStarted.contains(virtualMachine)) {
                vmsToBeStarted.remove(virtualMachine);
                vmStarted.add(virtualMachine);
            } else if (vmsToBeUpdated.contains(virtualMachine)) {
                vmsToBeUpdated.remove(virtualMachine);
                vmsUpdated.add(virtualMachine);
            } else {
                justUpdateDocker.add(virtualMachine);
            }
            virtualMachine.setDeplyoedContainers(xValueContainer.get(virtualMachine));
            databaseController.update(virtualMachine);
        }

        for (final VirtualMachine virtualMachine : vmsToBeStarted) {
            vmStarted.add(virtualMachine);
        }
        for (final VirtualMachine virtualMachine : vmsToBeUpdated) {
            vmsUpdated.add(virtualMachine);
            justUpdateDocker.add(virtualMachine);
        }


        for (VirtualMachine virtualMachine : vmsUpdated) {
            long startDate = virtualMachine.getStartDate();
            String sout = ("VM " + virtualMachine.getName() + "old start date: " + startDate);


            while (startDate + virtualMachine.getVmType().leasingDuration <= tau_t + 30000) {
                startDate = (startDate + virtualMachine.getVmType().leasingDuration);
            }

            virtualMachine.setToBeTerminatedAt(startDate + virtualMachine.getVmType().leasingDuration);

            LOG.info(sout + " new terminate " + virtualMachine.getToBeTerminatedAt() + " difference " + (virtualMachine.getToBeTerminatedAt() - tau_t));

            databaseController.update(virtualMachine);
            justUpdateDocker.add(virtualMachine);
        }

        controllerHandler.startVirtualMachine(vmStarted);

        for (final VirtualMachine virtualMachine : vmStarted) {
            virtualMachine.setDeplyoedContainers(xValueContainer.get(virtualMachine));
            virtualMachine.setStartDate(tau_t);
            virtualMachine.setToBeTerminatedAt(tau_t + virtualMachine.getVmType().leasingDuration);
            virtualMachine.setRunning(true);
            databaseController.update(virtualMachine);
        }

        controllerHandler.updateVirtualMachine(justUpdateDocker);
        List<VirtualMachine> all = new ArrayList<>();
        for (VirtualMachine virtualMachine : justUpdateDocker) {
            all.add(virtualMachine);
        }
        for (VirtualMachine virtualMachine : vmStarted) {
            all.add(virtualMachine);
        }

        try {
            if (all.size() > 0) {
                LOG.info("wait until VMs are up: " + all.size());
                Thread.sleep(20000L);
                //ensure docker containers are deployed
                controllerHandler.ensureDockerContainerAreDeployed(all);
            }
            //update ha proxy configuration
            controllerHandler.updateHAProxyConfiguration(all);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //remaining VMs to be started
        LOG.info("Empty VMs to be started?: " + vmsToBeStarted.size());

        //terminating old VMs
        Collection<List<VirtualMachine>> vmSet = controllerHandler.getVMMap(false).values();

        List<VirtualMachine> virtualMachines = new ArrayList<>();
        for (List<VirtualMachine> virtualMachineList : vmSet) {
            for (VirtualMachine virtualMachine : virtualMachineList) {
                if (virtualMachine.isRunning()) {

                    LOG.info(String.format("VM %s started: %s now: %s To be terminated at %s, difference in seconds: %s ",
                            virtualMachine.getName(),
                            virtualMachine.getStartDate(),
                            tau_t,
                            virtualMachine.getToBeTerminatedAt(),
                            (virtualMachine.getToBeTerminatedAt() - tau_t) / 1000));
                    if (virtualMachine.getToBeTerminatedAt() < tau_t) {
                        LOG.info("VM should be terminated " + virtualMachine.getName() + " VM_ID " + virtualMachine.getId());
                        virtualMachine.setRunning(false);
                        databaseController.update(virtualMachine);
                        controllerHandler.terminateVM(virtualMachine);
                    } else {
                        virtualMachines.add(virtualMachine);
                    }
                }
            }
        }


    }

    private void print(Result result, Number objective) {
        Map<DockerContainer, List<DockerContainer>> dockerMap = databaseController.getDockerMap();
        SortedMap<VMType, List<VirtualMachine>> vmMap = databaseController.getVmMap(false);

        Map<String, Number> xValues = new HashMap<>();
        Map<String, Number> yValues = new HashMap<>();
        Map<String, Number> betaValues = new HashMap<>();

        StringBuilder invocationOutput = new StringBuilder("");
        for (DockerContainer dockerContainerType : dockerMap.keySet()) {
            int invocations = databaseController.getInvocations(dockerContainerType);
            invocationOutput.append(dockerContainerType.getAppID()).append(": ")
                    .append(invocations).append("\n");
            for (DockerContainer dockerContainer : dockerMap.get(dockerContainerType)) {
                for (VMType vmType : vmMap.keySet()) {
                    for (VirtualMachine virtualMachine : vmMap.get(vmType)) {
                        String decisionVariableX = dockerPlacement.getDecisionVariableX(dockerContainer, virtualMachine);
                        xValues.put(decisionVariableX, result.get(decisionVariableX));

                        String decisionVariableY = dockerPlacement.getDecisionVariableY(virtualMachine);
                        yValues.put(decisionVariableY, result.get(decisionVariableY));

                        boolean running = virtualMachine.isRunning();
                        if (running) {
                            betaValues.put(dockerPlacement.getHelperVariableBeta(virtualMachine), 1);
                        }
                    }
                }
            }
        }


        StringBuilder y1Values = new StringBuilder("");
        StringBuilder y0Values = new StringBuilder("");
        StringBuilder x1Values = new StringBuilder("");
        StringBuilder beta1Values = new StringBuilder("");
        StringBuilder x0Values = new StringBuilder("");
        for (String variableName : yValues.keySet()) {
            Number value = yValues.get(variableName);
            if (value.intValue() == 0) {
                y0Values.append(variableName).append(";").append(value).append("\n");
            } else {
                y1Values.append(variableName).append(";").append(value).append("\n");
            }
        }
        for (String variableName : xValues.keySet()) {
            Number value = xValues.get(variableName);
            if (value.intValue() == 0) {
                x0Values.append(variableName).append(";").append(value).append("\n");
            } else {
                x1Values.append(variableName).append(";").append(value).append("\n");
            }
        }
        for (String variableName : betaValues.keySet()) {
            Number value = betaValues.get(variableName);
            if (value.intValue() == 0) {
                beta1Values.append(variableName).append(";").append(value).append("\n");
            } else {
                beta1Values.append(variableName).append(";").append(value).append("\n");
            }
        }


        LOG.info(""
                        + "\nplanned invocations: \n" + invocationOutput.toString()
                        + "\nBeta - 1 - values\n" + beta1Values.toString()
                        + "\nobjective: " + objective
                        + "\nY - 1 - values\n" + y1Values.toString()
                        + "\nX - 1 - values\n" + x1Values.toString()
        );

    }

    public void close() {
        LOG.info("shutting down all VMs");
        Collection<List<VirtualMachine>> vmSet = controllerHandler.getVMMap(true).values();

        for (List<VirtualMachine> virtualMachineList : vmSet) {
            for (VirtualMachine virtualMachine : virtualMachineList) {
                if (virtualMachine.isRunning()) {
                    controllerHandler.terminateVM(virtualMachine);
                }
            }
        }
    }
}
