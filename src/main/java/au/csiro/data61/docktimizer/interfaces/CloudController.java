package au.csiro.data61.docktimizer.interfaces;

import au.csiro.data61.docktimizer.controller.ControllerHandler;
import au.csiro.data61.docktimizer.exception.HardwareNotFoundException;
import au.csiro.data61.docktimizer.exception.ImageNotFoundException;
import au.csiro.data61.docktimizer.exception.NodeStartingException;
import au.csiro.data61.docktimizer.exception.UnsupportedVMNaming;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import com.google.common.base.Predicate;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunScriptOnNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.javax.annotation.Nullable;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jclouds.compute.options.TemplateOptions.Builder.overrideLoginCredentials;

/**
 */
public abstract class CloudController {

    protected static final Logger LOG = (Logger) LoggerFactory.getLogger(CloudController.class);

    protected ComputeServiceContext computeServiceApi;
    protected ComputeService compute;
    protected String DOCKER_RESIZE_SCRIPT;

    protected String vm_user_name;
    protected String vm_private_ssh_key;

    protected Map<String, Hardware> hardwareProfilesByName;
    protected Map<String, Hardware> hardwareProfilesByID;

    /**
     * starts a new VM
     *
     * @param virtualMachine to be started
     * @return the updated virtual machine object
     */
    public abstract VirtualMachine startInstance(VirtualMachine virtualMachine) throws NodeStartingException, HardwareNotFoundException, ImageNotFoundException, UnsupportedVMNaming;

    /**
     * loads the required properties and variables and initializes the controller
     */
    protected abstract void loadProperties() ;


    /**
     * @param virtualMachine to be terminated
     */
    public final void terminateInstance(final VirtualMachine virtualMachine) {
        if (ControllerHandler.DEBUG_MODE) {
            LOG.info("DEBUG MODE ON, just simulating termination");
            return;
        }
        Set<? extends NodeMetadata> nodeMetadatas = compute.destroyNodesMatching(new Predicate<NodeMetadata>() {
            @Override
            public boolean apply(@Nullable NodeMetadata input) {
                boolean contains = input.getName().contains("k" + virtualMachine.getPosition() + "-v" + virtualMachine.getVmType().cores);
                contains = (contains & virtualMachine.isTerminatable());
                return contains;
            }
        });
        for (NodeMetadata nodeMetadata : nodeMetadatas) {
            Log.info("VM terminated " + nodeMetadata.getName());
        }
    }

    /**
     * @return a list of currently running VMs
     */
    public abstract List<VirtualMachine> getServerList();

    /**
     * resizes a particular container on a virtual machine
     *
     * @param virtualMachine  target virtual machine
     * @param dockerContainer container with new configuration
     */
    public final DockerContainer resizeDocker(final VirtualMachine virtualMachine, DockerContainer dockerContainer) {
        if (ControllerHandler.DEBUG_MODE) {
            LOG.debug("just simulating resizing...");
            return dockerContainer;
        }
        String currentScript = DOCKER_RESIZE_SCRIPT;

        double vmCores = virtualMachine.getVmType().cores;
        double containerCores = dockerContainer.getContainerConfiguration().cores;
        long cpuShares = 1024 / (long) Math.ceil(vmCores / containerCores);

        currentScript = currentScript.replace("#{DOCKER_NAME}", dockerContainer.getDockerImage().getAppId());
        currentScript = currentScript.replace("#{CPU_SHARE}", String.valueOf(cpuShares));


        try {
            TemplateOptions options = overrideLoginCredentials(getLoginForCommandExecution()) // use my local user &  ssh key
                    .runAsRoot(false) // don't attempt to run as root (sudo)
                    .wrapInInitScript(false);
            Map<? extends NodeMetadata, ExecResponse> execResponseMap = compute.runScriptOnNodesMatching(new Predicate<NodeMetadata>() {
                @Override
                public boolean apply(@Nullable NodeMetadata nodeMetadata) {
                    return nodeMetadata.getName().contains("k" + virtualMachine.getPosition() + "-v" + virtualMachine.getVmType().cores);
                }
            }, currentScript, options);// run command directly
            for (NodeMetadata nodeMetadata : execResponseMap.keySet()) {
                ExecResponse execResponse = execResponseMap.get(nodeMetadata);
                LOG.info(execResponse.getOutput());
            }
            return dockerContainer;
        } catch (RunScriptOnNodesException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * terminates all virtual machine instances
     */
    protected final void terminateAllInstances() {
        Set<? extends NodeMetadata> nodeMetadatas = compute.destroyNodesMatching(new Predicate<NodeMetadata>() {
            @Override
            public boolean apply(@Nullable NodeMetadata nodeMetadata) {
                Matcher matcher = Pattern.compile("k[0-9]*-v[0-9]*").matcher(nodeMetadata.getName());
                boolean b = matcher.find();
                return b;
            }
        });
        for (NodeMetadata nodeMetadata : nodeMetadatas) {
            LOG.info("Node terminated: " + nodeMetadata.getName());
        }
    }


    /**
     * returns the hardware profile according a given flavor name
     *
     * @param flavorName e.g., m1.small, m1.micro, m1.medium, m2.medium, m3.large...
     * @return the hardware or throws exception
     * @throws HardwareNotFoundException if no hardware with this flavor name was found
     */
    protected final Hardware getHardware(String flavorName) throws HardwareNotFoundException {

        if (hardwareProfilesByName.isEmpty()) {
            loadHardwareProfiles();
        }

        Hardware hardware = hardwareProfilesByName.get(flavorName);
        if (hardware == null) {
            throw new HardwareNotFoundException("flavorName");
        }
        return hardware;
    }

    protected abstract void loadHardwareProfiles();


    public abstract void close() throws IOException;

    protected LoginCredentials getLoginForCommandExecution() {
        try {
            return LoginCredentials.builder().
                    user(vm_user_name).privateKey(vm_private_ssh_key).build();
        } catch (Exception e) {
            System.err.println("error reading ssh key " + e.getMessage());
            System.exit(1);
            return null;
        }
    }
}
