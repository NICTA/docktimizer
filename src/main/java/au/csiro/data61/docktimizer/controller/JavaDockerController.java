package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.exception.ContainerNotFoundException;
import au.csiro.data61.docktimizer.exception.CouldNotStartDockerException;
import au.csiro.data61.docktimizer.exception.CouldNotStopDockerException;
import au.csiro.data61.docktimizer.exception.CouldResizeDockerException;
import au.csiro.data61.docktimizer.interfaces.CloudController;
import au.csiro.data61.docktimizer.interfaces.DockerController;
import au.csiro.data61.docktimizer.models.DockerConfiguration;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.DockerImage;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.net.URI;
import java.util.*;

/**
 * This Docker controller makes use of the standard Docker REST API
 */
public class JavaDockerController implements DockerController {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(JavaDockerController.class);
    private static JavaDockerController instance;

    private Map<String, DefaultDockerClient> dockerClientMap;

    private Map<VirtualMachine, List<DockerContainer>> dockersPerVM;
    private String defaultPort;
    private CloudController cloudController;

    private JavaDockerController() {
        dockersPerVM = new HashMap<>();
        dockerClientMap = new HashMap<>();
    }

    public static JavaDockerController getInstance() {
        if (instance == null) {
            instance = new JavaDockerController();
        }
        return instance;

    }

    @Override
    public void initialize() {
        loadProperties();
        cloudController = ControllerHandler.getCloudControllerInstance();
    }

    @Override
    public Map<VirtualMachine, List<DockerContainer>> getDockersPerVM(boolean running) {
        return this.dockersPerVM;
    }

    @Override
    public List<DockerContainer> getDockers(VirtualMachine virtualMachine) throws Exception {
        List<DockerContainer> dockers = new ArrayList<>();
        DockerClient.ListContainersParam params = DockerClient.ListContainersParam.allContainers(false);

        DefaultDockerClient dockerClient = getDockerClient(virtualMachine);
        List<Container> containers = new ArrayList<>();
        try {
            containers = dockerClient.listContainers(params);
        } catch (Exception ex) {
            LOG.error("Could not get Docker containers: " + ex.getLocalizedMessage());
        }
        if (containers == null) {
            containers = new ArrayList<>();
        }
        for (Container container : containers) {
            String image = container.image();
            if (image.contains("swarm")) {
                continue;
            }
            List<String> names = container.names();

            DockerImage dockerImage = MysqlDatabaseController.parseByAppId(names.get(0));
            if (dockerImage == null) {
                LOG.info(image + " Unknown image ID ");
                continue;
            }
            DockerContainer con = new DockerContainer(dockerImage, DockerConfiguration.SINGLE_CORE);
            dockers.add(con);
        }
        return dockers;
    }

    @Override
    public DockerContainer startDocker(VirtualMachine virtualMachine, DockerContainer dockerContainer) throws CouldNotStartDockerException {
        if (ControllerHandler.DEBUG_MODE) {
            LOG.info("DEBUG MODE ON, just simulating deployment");
            try {
                Thread.sleep(dockerContainer.getDeployTime());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return dockerContainer;
        }

        DefaultDockerClient dockerClient = getDockerClient(virtualMachine);

        try {
            dockerClient.pull(dockerContainer.getDockerImage().getFullName(), new ProgressHandler() {
                @Override
                public void progress(ProgressMessage message) throws DockerException {
                    if (message != null && message.progress() != null) {
                        LOG.info(message.progress());
                    }
                }
            });


            final Map<String, List<PortBinding>> portBindings = new HashMap<>();
            List<PortBinding> hostPorts = new ArrayList<>();
            String externPort = String.valueOf(dockerContainer.getDockerImage().getExternPort());

            String internPort = String.valueOf(dockerContainer.getDockerImage().getInternPort());
            hostPorts.add(PortBinding.of("0.0.0.0", externPort));
            portBindings.put(internPort, hostPorts);

            final HostConfig hostConfig = HostConfig.builder().
                    portBindings(portBindings).build();

            double vmCores = virtualMachine.getVmType().cores;
            double containerCores = dockerContainer.getContainerConfiguration().cores;
            long cpuShares = 1024 / (long) Math.ceil(vmCores / containerCores);

            long memory = (long) dockerContainer.getContainerConfiguration().ram * 1024 * 1024;

            final ContainerConfig config = ContainerConfig.builder()
                    .image(dockerContainer.getDockerImage().getFullName())
                    .cpuShares(cpuShares)
                    .exposedPorts(String.valueOf(dockerContainer.getDockerImage().getInternPort()))
                    .memory(memory)
                    .env(String.format("WP_URL=\"%s:%s\"", virtualMachine.getIp(),
                            dockerContainer.getDockerImage().getExternPort()))
                    .build();

            String id;
            final String containerName = getContainerName(dockerContainer);

            try {
                final ContainerCreation creation = dockerClient.createContainer(config,
                        containerName);
                id = creation.id();
            } catch (DockerRequestException ex) {
                if (ex.message().contains("already in use")) {
                    ContainerInfo dockerInfo = getDockerInfo(virtualMachine, dockerContainer);
                    dockerContainer.setContainerID(dockerInfo.id());
                    if (!dockerInfo.state().running()) { //if not running, terminate
                        LOG.info("Could not create, already existing, let's remove it first");
                        stopDocker(virtualMachine, dockerContainer);
                        ContainerCreation creation = dockerClient.createContainer(config, containerName);
                        id = creation.id();
                    } else {
                        return resizeContainer(virtualMachine, dockerContainer);
                    }
                } else {
                    throw new Exception("Could not start container " + containerName, ex);
                }

            }

            dockerClient.startContainer(id, hostConfig);
            dockerContainer.setContainerID(id);


        } catch (Exception e) {
            //ignore
            LOG.info("could not start container " + virtualMachine.getName() + " message:  " + e.getMessage());
            return null;
        }

        return dockerContainer;
    }

    @Override
    public boolean stopDocker(VirtualMachine virtualMachine, DockerContainer dockerContainer) throws CouldNotStopDockerException {
        if (ControllerHandler.DEBUG_MODE) {
            LOG.info("DEBUG MODE ON, just simulating undeployment");
            try {
                Thread.sleep(dockerContainer.getDeployTime());
            } catch (InterruptedException ignore) {
            }
            return true;
        }

        DefaultDockerClient dockerClient = getDockerClient(virtualMachine);
        try {
            String containerID = getDockerID(dockerClient, dockerContainer);

            if (containerID == null) {
                LOG.debug("Container was not found, expected behaviour");
                return true;
            }
            dockerClient.stopContainer(containerID, 30);

            dockerClient.removeContainer(containerID);
        } catch (Exception e) {
            throw new CouldNotStopDockerException(e);
        }
        return true;
    }

    @Override
    public ContainerInfo getDockerInfo(VirtualMachine virtualMachine, DockerContainer dockerContainer) throws ContainerNotFoundException {
        DefaultDockerClient dockerClient = getDockerClient(virtualMachine);
        try {
            String containerID = getDockerID(dockerClient, dockerContainer);

            return dockerClient.inspectContainer(containerID);

        } catch (Exception e) {
            throw new ContainerNotFoundException(e);
        }
    }

    @Override
    public DockerContainer resizeContainer(VirtualMachine virtualMachine, DockerContainer newDockerContainer) throws CouldResizeDockerException {
        try {
            return cloudController.resizeDocker(virtualMachine, newDockerContainer);
        } catch (Exception e) {
            throw new CouldResizeDockerException(e);
        }
    }

    private String getDockerID(DefaultDockerClient dockerClient, DockerContainer dockerContainer) throws DockerException, InterruptedException {
        if (dockerContainer.getContainerID() != null) {
            return dockerContainer.getContainerID();
        }
        DockerClient.ListContainersParam params = DockerClient.ListContainersParam.allContainers(true);

        List<Container> containers = dockerClient.listContainers(params);
        for (Container container : containers) {
            if (container.names().toString().contains(getContainerName(dockerContainer))) {
                return container.id();
            }
        }
        return null;
    }

    private String getContainerName(DockerContainer dockerContainer) {
        return String.format("%s", dockerContainer.getDockerImage().getAppId());
    }

    private DefaultDockerClient getDockerClient(VirtualMachine virtualMachine) {
        DefaultDockerClient dockerClient = dockerClientMap.get(virtualMachine.getIp());
        if (dockerClient == null) {
            String url = String.format("http://%s:%s", virtualMachine.getIp(), defaultPort);
            dockerClient = DefaultDockerClient.builder().uri(URI.create(url)).build();
            dockerClientMap.put(virtualMachine.getIp(), dockerClient);
        }
        return dockerClient;
    }

    private void loadProperties() {
        Properties prop = new Properties();
        String openstack_properties = System.getenv("DOCKER_PROPERTY_FILE");
        try {
            if (openstack_properties != null) {
                LOG.info("DOCKER VARIABLE SET -- Loading from Environment Variable: ");
                LOG.info(openstack_properties);
                prop.load(new FileInputStream(openstack_properties));
            } else {
                openstack_properties = "docker-config/docker-swarm.properties";
                prop.load(getClass().getClassLoader().getResourceAsStream(openstack_properties));
            }
            defaultPort = prop.getProperty("DOCKER_SWARM_NODE_DEFAULT_PORT");

        } catch (Exception e) {
            LOG.error("\n-------------------------------------------------------" +
                    "\n------------docker properties not loaded-----------------" +
                    "\n---------------------------------------------------------" +
                    "\n--- /src/resources/docker-swarm.properties is missing ---" +
                    "\n---------------------------------------------------------");

        }
        LOG.info("\n---------------------------------------------------------" +
                "\n--------docker properties properties loaded---------------" +
                "\n----------------------------------------------------------");
    }

    public void setCloudController(CloudController cloudController) {
        this.cloudController = cloudController;
    }
}
