package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.AbstractTest;
import au.csiro.data61.docktimizer.interfaces.CloudController;
import au.csiro.data61.docktimizer.models.VMType;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import org.junit.*;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

/**
 *
 */
@Ignore("Run only manually, otherwise you might produce cost")
public class AWSCloudControllerTest extends AbstractTest {

    private static CloudController awsCloudController;

    @BeforeClass
    public static void setUp() throws Exception {
        ControllerHandler.DEBUG_MODE = false;
        awsCloudController = AWSCloudController.getInstance();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ControllerHandler.DEBUG_MODE = true;
    }

    @Test
    public void testGetInstance() throws Exception {
        List<VirtualMachine> serverList = awsCloudController.getServerList();
        assertThat(serverList.size(), is(greaterThanOrEqualTo(0)));
    }

    @Test
    public void testFullWorkflow() throws Exception {
        List<VirtualMachine> servers = awsCloudController.getServerList();
        int before = servers.size();
        assertThat(before, is(greaterThanOrEqualTo(0)));

        //add pre deployed container
        validVirtualMachine.addDockerContainer(bonomatContainer);
        validVirtualMachine.addDockerContainer(validDockerContainer);
        validVirtualMachine.setVmType(VMType.T2_MICRO);
        VirtualMachine virtualMachine = awsCloudController.startInstance(validVirtualMachine);
        assertThat(virtualMachine.getId(), is(notNullValue()));

        servers = awsCloudController.getServerList();
        int afterStart = servers.size();
        assertThat(afterStart, is(greaterThan(before)));

        awsCloudController.terminateInstance(virtualMachine);

        servers = awsCloudController.getServerList();
        int afterTerminate = servers.size();
        assertThat(afterTerminate, is(lessThan(afterStart)));
    }

}