package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.AbstractTest;
import au.csiro.data61.docktimizer.helper.HAProxyConfigGenerator;
import au.csiro.data61.docktimizer.interfaces.LoadBalancerController;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertTrue;

public class HAProxyLoadBalancerControllerTest extends AbstractTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(3001);

    @Before
    public void setupLoadBalancerMock() {
        stubFor(post(urlEqualTo("/api/files"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>")));


    }

    @Test
    public void testAddServer() throws Exception {
        List<DockerContainer> list = new ArrayList<>();
        list.add(validDockerContainer);
        validVirtualMachine.setDeplyoedContainers(list);
        List<VirtualMachine> vms = new ArrayList<>();
        vms.add(validVirtualMachine);

        LoadBalancerController controller = HAProxyLoadBalancerController.getInstance();
        controller.initialize();

        String postBody = HAProxyConfigGenerator.generateConfigFile(vms);

        Integer result = controller.updateHAProxyConfiguration(vms);
        assertTrue(result == 200);

        verify(postRequestedFor(urlMatching("/api/files"))
                .withRequestBody(containing(postBody)));

    }


}