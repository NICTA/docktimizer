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
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.inject.Module;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.internal.NodeMetadataImpl;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.sshj.config.SshjSshClientModule;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cloud controller for Amazon EC2
 */
public class AWSCloudController extends CloudController {

    private static AWSCloudController instance;


    private String cloud_default_key_pair;
    private String cloud_access_key_id;
    private String cloud_secret_key;
    private String cloud_zone;
    private String cloud_default_image_id;
    private String cloud_public_key;
    private boolean setupInfoLoaded;
    private String CLOUD_INIT;
    private String CLOUD_INIT_DOCKER_START_TEMPL;
    private String cloud_security_group;

    private AmazonEC2Client ec2;

    private AWSCloudController() {
        hardwareProfilesByName = new HashMap<>();
        hardwareProfilesByID = new HashMap<>();
    }

    protected synchronized static CloudController getInstance() {
        if (instance == null) {
            instance = new AWSCloudController();
            instance.initialize();
        }
        return instance;
    }

    private void initialize() {
        loadProperties();
        LOG.info("initializing cloud controller... ");
        String provider = "aws-ec2";

        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule(), new SshjSshClientModule());

        Properties overrides = new Properties();
        overrides.setProperty("jclouds.regions", "us-west-1");

        computeServiceApi = ContextBuilder.newBuilder(provider)
                .credentials(cloud_access_key_id, cloud_secret_key)
                .overrides(overrides)
                .buildApi(ComputeServiceContext.class);

        ComputeServiceContext context = ContextBuilder.newBuilder(provider)
                .credentials(cloud_access_key_id, cloud_secret_key)
                .modules(modules)
                .buildView(ComputeServiceContext.class);
        compute = context.getComputeService();


        /**
         * AWS API Part
         */

        AWSCredentials credentials = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return cloud_access_key_id;
            }

            @Override
            public String getAWSSecretKey() {
                return cloud_secret_key;
            }
        };

        ec2 = new AmazonEC2Client(credentials);
        Region defaultRegion = Region.getRegion(Regions.fromName(cloud_zone));
        ec2.setRegion(defaultRegion);

        LOG.info("Successfully connected to AWS");


        //load hardware profiles
        if (!setupInfoLoaded) {
            loadHardwareProfiles();
            setupInfoLoaded = true;
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

            cloud_access_key_id = prop.getProperty("CLOUD_ACCESS_KEY_ID");
            cloud_secret_key = prop.getProperty("CLOUD_USER_PASSWORD");
            cloud_default_key_pair = prop.getProperty("CLOUD_DEFAULT_KEY_PAIR");
            cloud_zone = prop.getProperty("CLOUD_ZONE");
            cloud_default_image_id = prop.getProperty("CLOUD_DEFAULT_IMAGE_ID");
            vm_user_name = prop.getProperty("VM_USER_NAME");
            vm_private_ssh_key = prop.getProperty("VM_PRIVATE_SSH_KEY");
            cloud_security_group = prop.getProperty("CLOUD_SECURITY_GROUP");
            cloud_public_key = prop.getProperty("VM_PUBLIC_SSH_KEY");

            FileLoader fileLoader = new FileLoader();
            CLOUD_INIT = fileLoader.CLOUD_INIT;
            CLOUD_INIT_DOCKER_START_TEMPL = fileLoader.CLOUD_INIT_DOCKER_START;
            DOCKER_RESIZE_SCRIPT = fileLoader.DOCKER_RESIZE_SCRIPT;

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
        String ipAddress = startVM(virtualMachine);
        virtualMachine.setIp(ipAddress);

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


    /**
     * @return a list of VMs
     * NOTE: only VMs with name k[0-9]*-v[0-9]* are accepted
     */
    @Override
    public List<VirtualMachine> getServerList() {
        Set<? extends ComputeMetadata> computeMetadatas = compute.listNodes();

        List<VirtualMachine> virtualMachineList = new ArrayList<>();

        for (ComputeMetadata computeMetadata : computeMetadatas) {
            String serverName = computeMetadata.getName();
            Matcher matcher = Pattern.compile("k[0-9]*-v[0-9]*").matcher(serverName);
            boolean isOneOfTheSwarm = matcher.find();
            if (!isOneOfTheSwarm) {
                continue;
            }
            String s = StringUtils.substringBetween(serverName, "k", "-v");
            Integer position = Integer.parseInt(s);

            String flavorId = ((NodeMetadataImpl) computeMetadata).getHardware().getId();
            VMType byFlavorName = VMType.getByFlavorName(flavorId);
            try {
                Iterator<String> iterator = ((NodeMetadataImpl) computeMetadata).getPublicAddresses().iterator();
                if (iterator.hasNext()) {
                    String ipAddress = iterator.next();
                    VirtualMachine vm = new VirtualMachine(serverName, byFlavorName, position, ipAddress);
                    virtualMachineList.add(vm);
                }
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
            hardwareProfilesByName.put(profile.getId(), profile);
            hardwareProfilesByID.put(profile.getProviderId(), profile);
        }
    }


    @Override
    public void close() throws IOException {
        Closeables.close(computeServiceApi, true);
    }


    public String startVM(VirtualMachine virtualMachine) {

        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        String dockerStartups = generateDockerStartupScripts(virtualMachine);
        String nodeSpecificCloudInit = CLOUD_INIT.replace("#{DOCKER-UNITS}", dockerStartups);

        runInstancesRequest.withImageId(cloud_default_image_id)
                .withInstanceType(virtualMachine.getVmType().awsFlavor)
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(cloud_default_key_pair)
                .withSecurityGroups(cloud_security_group)
                .withUserData(Base64.encodeBase64String(nodeSpecificCloudInit.getBytes()));

        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);

        String instanceId = null;
        Reservation reservation = runInstancesResult.getReservation();
        List<Instance> instances = reservation.getInstances();
        for (Instance ii : instances) {
            if (ii.getState().getName().equals("pending") || ii.getState().getName().equals("running")) {
                instanceId = ii.getInstanceId();
                String name = "k" + virtualMachine.getPosition() + "-v" + virtualMachine.getVmType().cores;

                CreateTagsRequest createTagsRequest = new CreateTagsRequest();
                createTagsRequest.withResources(instanceId).withTags(new Tag("Name", name));
                ec2.createTags(createTagsRequest);
            }
        }

        boolean isWaiting = true;
        while (isWaiting) {//wait until the IP address is awailable
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DescribeInstancesResult r = ec2.describeInstances();
            for (Reservation rr : r.getReservations()) {
                List<Instance> instances2 = rr.getInstances();
                for (Instance instance : instances2) {

                    if (instance.getState().getName().equals("running") && instance.getInstanceId().equals(instanceId)) {
                        LOG.info("AWS instance " + instance.getInstanceId() + " and public IP " + instance.getPublicIpAddress() + " was started");
                        return instance.getPublicIpAddress();
                    }
                }
            }
        }
        return null;
    }
}
