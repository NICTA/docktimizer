package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.AbstractTest;
import au.csiro.data61.docktimizer.helper.HAProxyConfigGenerator;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import au.csiro.data61.docktimizer.testClient.DockerPlacementServiceTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class HAProxyLoadBalancerControllerIT extends AbstractTest {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(DockerPlacementServiceTest.class);


    @Test
    public void testAddServer() throws Exception {
        List<DockerContainer> list = new ArrayList<>();
        list.add(bonomatContainer);
        list.add(joomlaDockerContainer);
        list.add(validDockerContainer);
        validVirtualMachine.setDeplyoedContainers(list);
        List<VirtualMachine> vms = new ArrayList<>();
        invalidVirtualMachine.setDeplyoedContainers(list);
        invalidVirtualMachine.setIp("10.0.0.1");
        vms.add(validVirtualMachine);
        vms.add(invalidVirtualMachine);
        validVirtualMachine.setIp("10.99.0.12");
        HAProxyLoadBalancerController controller = (HAProxyLoadBalancerController) HAProxyLoadBalancerController.getInstance();

        controller.initialize();
        controller.LOAD_BALANCER_UPDATE_URL = "http://localhost:3001/api/files";

        String postBody = HAProxyConfigGenerator.generateConfigFile(vms);
        LOG.info(postBody);
        Integer result = controller.updateHAProxyConfiguration(vms);
        assertTrue(result == 200);


    }


}