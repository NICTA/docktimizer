package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.AbstractTest;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VMType;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MysqlDatabaseControllerTest extends AbstractTest {

    private static MysqlDatabaseController dbController;

    @BeforeClass
    public static void setUp() throws Exception {
        dbController = MysqlDatabaseController.getInstance();
        dbController.initializeAndUpdate(new Date().getTime());


    }

    @Test
    public void testGetVmMap() throws Exception {
        SortedMap<VMType, List<VirtualMachine>> vmMap = dbController.getVmMap(false);
        assertThat(vmMap.size(), is(MysqlDatabaseController.V));

        for (VMType vmType : vmMap.keySet()) {
            List<VirtualMachine> containerList = vmMap.get(vmType);
            assertThat(containerList.size(), is(MysqlDatabaseController.K));
        }
    }

    @Test
    public void testGetDockerMap() throws Exception {
        Map<DockerContainer, List<DockerContainer>> dockerMap = dbController.getDockerMap();
        assertThat(dockerMap.size(), is(MysqlDatabaseController.D));
        for (DockerContainer dockerContainer : dockerMap.keySet()) {
            List<DockerContainer> containerList = dockerMap.get(dockerContainer);
            assertThat(containerList.size(), is(MysqlDatabaseController.C));
        }
    }

    @Test
    public void testGetVmMap_AndUpdate() throws Exception {
        SortedMap<VMType, List<VirtualMachine>> vmMap = dbController.getVmMap(false);
        assertThat(vmMap.size(), is(MysqlDatabaseController.V));

        Map<DockerContainer, List<DockerContainer>> dockerMap = dbController.getDockerMap();
        DockerContainer one = null;
        for (DockerContainer dockerContainer : dockerMap.keySet()) {
            List<DockerContainer> containerList = dockerMap.get(dockerContainer);
            one = containerList.get(0);
            break;
        }

        for (VMType vmType : vmMap.keySet()) {
            List<VirtualMachine> virtualMachines = vmMap.get(vmType);
            assertThat(virtualMachines.size(), is(MysqlDatabaseController.K));
            virtualMachines.get(0).addDockerContainer(one);
            dbController.update(virtualMachines.get(0));
        }


        dbController = MysqlDatabaseController.getInstance();
        dbController.initializeAndUpdate(new Date().getTime());
        vmMap = dbController.getVmMap(false);
        assertThat(vmMap.size(), is(MysqlDatabaseController.V));

        int deployedContainer = 0;
        for (VMType vmType : vmMap.keySet()) {
            List<VirtualMachine> virtualMachines = vmMap.get(vmType);
            assertThat(virtualMachines.size(), is(MysqlDatabaseController.K));
            for (VirtualMachine virtualMachine : virtualMachines) {
                deployedContainer += virtualMachine.getDeployedContainers().size();
            }
        }
        assertThat(deployedContainer, is(MysqlDatabaseController.V));
    }

    @Test
    public void getDockerByAppId() {
        DockerContainer docker = dbController.getDocker(validDockerContainer.getAppID());
        assertThat(docker == null, is(false));
    }
}