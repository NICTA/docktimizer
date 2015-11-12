package au.csiro.data61.docktimizer.helper;

import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class HAProxyConfigGenerator {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(HAProxyConfigGenerator.class);

    private static final String TEMPL_SETTINGS = "#{TEMPL_SETTINGS}";
    private static final String TEMPL_FRONTEND = "#{FRONTEND_SETTINGS}";
    private static final String TEMPL_BACKENDS = "#{BACKEND_SETTINGS}";
    private static final String TEMPL_NAME = "#{NAME}";
    private static final String TEMPL_IP = "#{IP}";
    private static final String TEMPL_PORT = "#{PORT}";
    private static final String TEMPL_COUNTER = "#{COUNTER}";
    private static final String MAX_CONNECTIONS = "#{MAX_CONNECTIONS}";
    private static final String TEMPL_ACLs = "#{ACLs}";
    private static final String SERVER_SHORT = "#{SERVER}";
    private static final String BACKEND_TEMPLATE;
    private static final String CONFIGURATION_TEMPLATE;
    private static final String ACL_TEMPLATE;
    private static final String FRONTEND_TEMPLATE;
    private static final String SERVER_TEMPLATE;

    static  {
        String template = "";
        String fullConfig = "";
        String acl = "";
        String frontend = "";
        String server = "";
        try {
            template = IOUtils.toString(
                    HAProxyConfigGenerator.class.getClassLoader().getResourceAsStream("ha-templates/haproxy.backend.template"),
                    "UTF-8");
            fullConfig = IOUtils.toString(
                    HAProxyConfigGenerator.class.getClassLoader().getResourceAsStream("ha-templates/haproxy.cfg"),
                    "UTF-8");
            acl = IOUtils.toString(
                    HAProxyConfigGenerator.class.getClassLoader().getResourceAsStream("ha-templates/haproxy.fontend-acl.template"),
                    "UTF-8");
            frontend = IOUtils.toString(
                    HAProxyConfigGenerator.class.getClassLoader().getResourceAsStream("ha-templates/haproxy.fontend.template"),
                    "UTF-8");
            server = IOUtils.toString(
                    HAProxyConfigGenerator.class.getClassLoader().getResourceAsStream("ha-templates/server.template"),
                    "UTF-8");
        } catch (Exception e) {
            System.err.println("Could not load cloud-init config");
        }
        BACKEND_TEMPLATE = template;
        CONFIGURATION_TEMPLATE = fullConfig;
        ACL_TEMPLATE = acl;
        FRONTEND_TEMPLATE = frontend;
        SERVER_TEMPLATE  = server;
    }

    public static String generateConfigFile(List<VirtualMachine> virtualMachines) {
        StringBuilder backends = new StringBuilder("");
        StringBuilder aclList = new StringBuilder("");

        Map<DockerContainer, List<VirtualMachine>> serverPerApp = new HashMap<>();

        for (VirtualMachine virtualMachine : virtualMachines) {
            List<DockerContainer> deployedContainers = virtualMachine.getDeployedContainers();
            for (DockerContainer deployedContainer : deployedContainers) {
                List<VirtualMachine> virtualMachines1 = serverPerApp.get(deployedContainer);
                if (virtualMachines1 == null) {
                    virtualMachines1 = new ArrayList<>();
                }
                virtualMachines1.add(virtualMachine);
                serverPerApp.put(deployedContainer, virtualMachines1);
            }
        }

        for (DockerContainer dockerContainer : serverPerApp.keySet()) {
            int counter = 0;
            String newBackendString = updateBackendTemplate(dockerContainer, counter);
            StringBuilder tmp = new StringBuilder("");
            for (VirtualMachine virtualMachine : serverPerApp.get(dockerContainer)) {
                counter++;
                String str = updateServerTemplate(virtualMachine, dockerContainer, counter);
                tmp.append(str).append("\n");
            }
            String replace = newBackendString.replace(SERVER_SHORT, tmp.toString());
            backends.append("\n############### next server #############\n");
            backends.append(replace);
        }

        int counter = 0;
        for (DockerContainer appId : serverPerApp.keySet()) {
            aclList.append(updateACLTemplate(appId.getAppID()));
            if (!(counter == serverPerApp.keySet().size()-1)) {
                aclList.append("\n");
            }
            counter++;
        }
        String frontendsACL = FRONTEND_TEMPLATE.replace(TEMPL_ACLs, aclList.toString());

        String inclFrontend = CONFIGURATION_TEMPLATE.replace(TEMPL_FRONTEND, frontendsACL);
        String inclBackend = inclFrontend.replace(TEMPL_BACKENDS, backends.toString());


        return inclBackend;
    }

    private static String updateACLTemplate(String appId) {
        String newACL = ACL_TEMPLATE;
        newACL = newACL.replace(TEMPL_NAME, appId);
        return newACL;
    }

    protected static String updateBackendTemplate(DockerContainer dockerContainer, int counter) {
        if (dockerContainer.getAppID() == null ) {
            return "";
        }
        String newServerString = BACKEND_TEMPLATE;
        newServerString = newServerString.replace(TEMPL_NAME, dockerContainer.getAppID());
        return newServerString;
    }

    protected static String updateServerTemplate(VirtualMachine virtualMachine, DockerContainer dockerContainer, int counter) {
        if (dockerContainer.getAppID() == null || virtualMachine.getIp() == null) {
            return "";
        }
        String newServerString = SERVER_TEMPLATE;
        newServerString = newServerString.replace(TEMPL_NAME, dockerContainer.getAppID());
        newServerString = newServerString.replace(TEMPL_IP, virtualMachine.getIp());
        newServerString = newServerString.replace(TEMPL_PORT, dockerContainer.getDockerImage().getExternPort() + "");
        newServerString = newServerString.replace(TEMPL_COUNTER, counter + "");
        newServerString = newServerString.replace(MAX_CONNECTIONS, dockerContainer.getAmountOfPossibleInvocations()+"");
        return newServerString;
    }
}
