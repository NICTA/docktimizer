package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.AbstractTest;
import au.csiro.data61.docktimizer.exception.HardwareNotFoundException;
import au.csiro.data61.docktimizer.exception.ImageNotFoundException;
import au.csiro.data61.docktimizer.exception.NodeStartingException;
import au.csiro.data61.docktimizer.exception.UnsupportedVMNaming;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 */
@Ignore("This test should only be run manually as it uses cloud resources and may produce cost")
public class OpenstackCloudControllerITest extends AbstractTest {

    @Before
    public void before() {
        ControllerHandler.DEBUG_MODE = false;
        cloudController = OpenstackCloudController.getInstance();
    }

    @After
    public void after() {
        ControllerHandler.DEBUG_MODE = true;
    }

    @Test
    public void getServerList() {
        List<VirtualMachine> servers = cloudController.getServerList();
        int before = servers.size();
        assertThat(before, is(greaterThanOrEqualTo(0)));
    }

    @Test
    public void startVirtualMachine() throws NodeStartingException, ImageNotFoundException, UnsupportedVMNaming, HardwareNotFoundException {


        List<VirtualMachine> servers = cloudController.getServerList();
        int before = servers.size();
        assertThat(before, is(greaterThanOrEqualTo(0)));

        //add pre deployed container
        validVirtualMachine.addDockerContainer(bonomatContainer);
        validVirtualMachine.addDockerContainer(validDockerContainer);

        VirtualMachine virtualMachine = cloudController.startInstance(validVirtualMachine);
        assertThat(virtualMachine.getId(), is(notNullValue()));

        servers = cloudController.getServerList();
        int afterStart = servers.size();
        assertThat(afterStart, is(greaterThan(before)));

        cloudController.terminateInstance(virtualMachine);

        servers = cloudController.getServerList();
        int afterTerminate = servers.size();
        assertThat(afterTerminate, is(lessThan(afterStart)));

    }

    @Test
    public void getExistingServers() {
        List<VirtualMachine> serverList = cloudController.getServerList();

    }

    @Test
    public void terminateAllVms() {
        List<VirtualMachine> serverList = cloudController.getServerList();
        for (VirtualMachine virtualMachine : serverList) {
            cloudController.terminateInstance(virtualMachine);

        }
        List<VirtualMachine> servers = cloudController.getServerList();
        int before = servers.size();
        assertThat(before, is(0));
    }


}
