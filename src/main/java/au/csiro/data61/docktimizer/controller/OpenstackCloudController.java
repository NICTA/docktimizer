package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.exception.HardwareNotFoundException;
import au.csiro.data61.docktimizer.exception.ImageNotFoundException;
import au.csiro.data61.docktimizer.exception.NodeStartingException;
import au.csiro.data61.docktimizer.exception.UnsupportedVMNaming;
import au.csiro.data61.docktimizer.helper.FileLoader;
import au.csiro.data61.docktimizer.interfaces.CloudController;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VMType;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;
import org.apache.commons.lang3.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.RunScriptOnNodesException;
import org.jclouds.compute.domain.*;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jclouds.compute.options.TemplateOptions.Builder.overrideLoginCredentials;

/**
 */
public class OpenstackCloudController extends CloudController {

    private static OpenstackCloudController instance;
    private NovaApi novaApi;


    private Map<String, Image> imageProfiles;
    private String cloud_user_name;
    private String cloud_user_password;
    private String cloud_default_key_pair;
    private String cloud_endpoint;
    private String cloud_tenant_name;
    private String cloud_identity;
    private String cloud_zone;
    private String cloud_default_image_id;
    private String vm_user_name;
    private String vm_private_ssh_key;
    private boolean setupInfoLoaded;
    private Image defaultImage;
    private String DOCKER_RESIZE_SCRIPT;
    private String CLOUD_INIT;
    private String CLOUD_INIT_DOCKER_START_TEMPL;
    private String cloud_security_group;

    private OpenstackCloudController() {
        hardwareProfilesByName = new HashMap<>();
        imageProfiles = new HashMap<>();
        hardwareProfilesByID = new HashMap<>();

    }

    protected static CloudController getInstance() {
        if (instance == null) {
            instance = new OpenstackCloudController();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        loadProperties();
        LOG.info("initializing cloud controller... ");
        String provider = "openstack-nova";

        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule(), new SshjSshClientModule());


        novaApi = ContextBuilder.newBuilder(provider)
                .endpoint(cloud_endpoint)
                .credentials(cloud_identity, cloud_user_password)
                .modules(modules)
                .buildApi(NovaApi.class);

        ComputeServiceContext context = ContextBuilder.newBuilder(provider)
                .endpoint("http://openstack.infosys.tuwien.ac.at/identity/v2.0")
                .credentials(cloud_identity, cloud_user_password)
                .modules(modules)
                .buildView(ComputeServiceContext.class);
        compute = context.getComputeService();


        //load hardware profiles
        if (!setupInfoLoaded) {
            loadHardwareProfiles();
            loadImageProfiles();
            setupInfoLoaded = true;
        }

        try {
            if (defaultImage == null) {
                defaultImage = getImage(cloud_default_image_id);
            }
        } catch (ImageNotFoundException e) {
            LOG.error("Could not load image");
        }
    }


    protected void loadProperties() {
        Properties prop = new Properties();
        String openstack_properties = System.getenv("OPENSTACK_PROPERTY_FILE");
        try {
            if (openstack_properties != null) {
                LOG.info("OPENSTACK VARIABLE SET -- Loading from Environment Variable: ");
                LOG.info(openstack_properties);
                prop.load(new FileInputStream(openstack_properties));
            } else {
                openstack_properties = "cloud-config/cloud-config.properties";
                prop.load(getClass().getClassLoader().getResourceAsStream(openstack_properties));
            }

            cloud_user_name = prop.getProperty("CLOUD_USER_NAME");
            cloud_user_password = prop.getProperty("CLOUD_USER_PASSWORD");
            cloud_default_key_pair = prop.getProperty("CLOUD_DEFAULT_KEY_PAIR");
            cloud_endpoint = prop.getProperty("CLOUD_ENDPOINT");
            cloud_tenant_name = prop.getProperty("CLOUD_TENANT_NAME");
            cloud_zone = prop.getProperty("CLOUD_ZONE");
            cloud_default_image_id = prop.getProperty("CLOUD_DEFAULT_IMAGE_ID");
            vm_user_name = prop.getProperty("VM_USER_NAME");
            vm_private_ssh_key = prop.getProperty("VM_PRIVATE_SSH_KEY");
            cloud_security_group = prop.getProperty("CLOUD_SECURITY_GROUP");

            FileLoader fileLoader = new FileLoader();
            CLOUD_INIT = fileLoader.CLOUD_INIT;
            CLOUD_INIT_DOCKER_START_TEMPL = fileLoader.CLOUD_INIT_DOCKER_START;
            DOCKER_RESIZE_SCRIPT = fileLoader.DOCKER_RESIZE_SCRIPT;

            cloud_identity = cloud_tenant_name + ":" + cloud_user_name;
        } catch (Exception e) {
            LOG.error("\n---------------------------------------------------------" +
                    "\n--------openstack properties not loaded------------------" +
                    "\n---------------------------------------------------------" +
                    "\n--- /src/resources/cloud-config.properties is missing ---" +
                    "\n---------------------------------------------------------");

        }
        LOG.info("\n---------------------------------------------------------" +
                "\n--------openstack properties loaded----------------------" +
                "\n---------------------------------------------------------");
    }

    /**
     * starts a new instance
     *
     * @param virtualMachine to be started
     * @return the virtual machine object
     * @throws NodeStartingException     if node could not be started
     * @throws HardwareNotFoundException if the given flavor name was not found
     * @throws ImageNotFoundException    if the given imageID was not found
     * @throws UnsupportedVMNaming       if the given VM name is invalid
     */
    @Override
    public VirtualMachine startInstance(VirtualMachine virtualMachine) throws NodeStartingException, HardwareNotFoundException, ImageNotFoundException, UnsupportedVMNaming {
        if (ControllerHandler.DEBUG_MODE) {
            LOG.info("Just simulating VM instantiation...");
            try {
                Thread.sleep(30000L);
                virtualMachine.setIp("128.130.172.209");
                return virtualMachine;
            } catch (InterruptedException ignore) {
            }
        }

        String dockerStartups = generateDockerStartupScripts(virtualMachine);
        String nodeSpecificCloudInit = CLOUD_INIT.replace("#{DOCKER-UNITS}", dockerStartups);


        TemplateOptions options = NovaTemplateOptions.Builder
                .userData(nodeSpecificCloudInit.getBytes())
                .keyPairName(cloud_default_key_pair)
                .securityGroups(cloud_security_group);
        Template template = compute.templateBuilder()
                .locationId(cloud_zone)
                .options(options)
                .fromHardware(getHardware(virtualMachine.getVmType().flavorName))
                .fromImage(defaultImage)
                .build();

        // This method will continue to poll for the server status and won't return until this server is ACTIVE
        // If you want to know what's happening during the polling, enable logging. See
        // /jclouds-example/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
        Set<? extends NodeMetadata> nodes = null;
        String name = "k" + virtualMachine.getPosition() + "-v" + virtualMachine.getVmType().cores;

        Matcher matcher = Pattern.compile("k[0-9]*-v[0-9]*").matcher(name);
        boolean b = matcher.find();
        if (!b) {
            throw new UnsupportedVMNaming(name + " is not supported. Naming should have the format k[0-9]*v[0-9]*");
        }

        try {
            nodes = compute.createNodesInGroup(name, 1, template);

        } catch (RunNodesException e) {
            e.printStackTrace();
            throw new NodeStartingException(e);
        }

        NodeMetadata nodeMetadata = nodes.iterator().next();
        String publicAddress = nodeMetadata.getPrivateAddresses().iterator().next();

        virtualMachine.setIp(publicAddress);

        return virtualMachine;
    }

    private String generateDockerStartupScripts(VirtualMachine virtualMachine) {
        StringBuilder startups = new StringBuilder("");
        double vmCores = virtualMachine.getVmType().cores;

        for (DockerContainer deployedContainer : virtualMachine.getDeployedContainers()) {
            double containerCores = deployedContainer.getContainerConfiguration().cores;
            long cpuShares = 1024 / (long) Math.ceil(vmCores / containerCores);

            String replace = CLOUD_INIT_DOCKER_START_TEMPL.replace("#{name}", deployedContainer.getAppID());
            String runCmd = String.format("/usr/bin/docker run --restart=\"always\" --name %s -p %s:%s --cpu-shares=%s %s",
                    deployedContainer.getDockerImage().getAppId(),
                    deployedContainer.getDockerImage().getExternPort(),
                    deployedContainer.getDockerImage().getInternPort(),
                    cpuShares,
                    deployedContainer.getDockerImage().getFullName());

            replace = replace.replace("#{RUN-CMD}", runCmd);
            startups.append(replace).append("\n");
        }
        return startups.toString();
    }

    @Override
    public List<VirtualMachine> getServerList() {
        ServerApi serverApi = novaApi.getServerApiForZone(cloud_zone);
        List<VirtualMachine> virtualMachineList = new ArrayList<>();

        for (Server server : serverApi.listInDetail().concat()) {

            String serverName = server.getName();
            Matcher matcher = Pattern.compile("k[0-9]*-v[0-9]*").matcher(serverName);
            boolean isOneOfTheSwarm = matcher.find();
            if (!isOneOfTheSwarm) {
                continue;
            }

            String flavorID = server.getFlavor().getId();
            Hardware hardware = hardwareProfilesByID.get(flavorID);

            String s = StringUtils.substringBetween(serverName, "k", "-v");
            Integer position = Integer.parseInt(s);
            VMType byFlavorName = VMType.getByFlavorName(hardware.getName());
            try {
                String ipAddress = server.getAddresses().values().iterator().next().getAddr();
                VirtualMachine vm = new VirtualMachine(serverName,
                        byFlavorName, position, ipAddress);
                virtualMachineList.add(vm);
            } catch (Exception ex) {
                //ignore
            }
        }
        return virtualMachineList;
    }

    @Override
    protected void loadHardwareProfiles() {
        Set<? extends Hardware> profiles = compute.listHardwareProfiles();
        for (Hardware profile : profiles) {
            hardwareProfilesByName.put(profile.getName(), profile);
            hardwareProfilesByID.put(profile.getProviderId(), profile);
        }
    }


    /**
     * This method uses the generic ComputeService.listImages() to find the image.
     *
     * @param imageID in form of a UUID
     * @return the image for that ID
     */
    private Image getImage(String imageID) throws ImageNotFoundException {

        if (imageProfiles.isEmpty()) {
            loadImageProfiles();
        }

        Image image = imageProfiles.get(imageID);
        if (image == null) {
            throw new ImageNotFoundException(imageID);
        }

        return image;
    }

    private void loadImageProfiles() {
        Set<? extends Image> images = compute.listImages();
        for (Image image : images) {
            imageProfiles.put(image.getProviderId(), image);
        }
    }

    @Override
    public void close() throws IOException {
        Closeables.close(novaApi, true);
    }


}
