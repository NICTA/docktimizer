package au.csiro.data61.docktimizer.testClient;

import au.csiro.data61.docktimizer.controller.MysqlDatabaseController;
import au.csiro.data61.docktimizer.interfaces.DatabaseController;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.PlannedInvocation;
import au.csiro.data61.docktimizer.models.VMType;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import au.csiro.data61.docktimizer.placement.DockerPlacementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.util.*;

/**
 * Unit test for simple App.
 */
public class DockerPlacementServiceTest {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(DockerPlacementServiceTest.class);
    private DockerPlacementService dockerPlacementService;
    private DatabaseController databaseController;

    public static void main(String[] args) {
        LOG.info("Getting started, waiting for your <enter>");
        Console console = System.console();
        if (console != null) {
            String input = console.readLine("Press enter to start");
        }

        DockerPlacementServiceTest test = new DockerPlacementServiceTest();
        try {
            try {
                test.setUp();
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Could not setup, exiting");
            }
            try {
                test.solve();
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Could not solve, exiting");
            }
            LOG.info("Worked :)");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            test.tearDown();
            System.exit(-1);
        }


    }

    private void tearDown() {
        databaseController.close();
        dockerPlacementService.close();
    }

    public void setUp() throws Exception {
        databaseController = MysqlDatabaseController.getInstance();
        long tau_t = new Date().getTime();
        databaseController.initializeAndUpdate(tau_t);

        dockerPlacementService = DockerPlacementService.getInstance();

        SortedMap<VMType, List<VirtualMachine>> vmMap = databaseController.getVmMap(true);
        boolean saved = false;
        for (VMType vmType : vmMap.keySet()) {

            switch (vmType) {
                case M1_LARGE:
                    if (!saved) {
                        List<VirtualMachine> virtualMachines = vmMap.get(VMType.M1_LARGE);
                        VirtualMachine virtualMachine = virtualMachines.get(0);
                        long startDate = tau_t - (5 * 60 * 900);
                        virtualMachine.setStartDate(startDate);
                        virtualMachine.setToBeTerminatedAt(startDate + virtualMachine.getVmType().leasingDuration);
                        virtualMachine.setRunning(true);
                        Set<DockerContainer> dockerContainers = databaseController.getDockerMap().keySet();

                        Iterator<DockerContainer> iterator = dockerContainers.iterator();
                        virtualMachine.addDockerContainer(iterator.next());
                        virtualMachine.addDockerContainer(iterator.next());
                        virtualMachine.addDockerContainer(iterator.next());
                        databaseController.update(virtualMachine);
                        long remaining = virtualMachine.getToBeTerminatedAt() - tau_t;
                        saved = true;
                    }
                    break;
            }
        }

        Map<DockerContainer, List<DockerContainer>> dockerMap = databaseController.getDockerMap();
        for (int i = 0; i < 1; i++) {

            for (DockerContainer dockerContainer : dockerMap.keySet()) {

                int amount = 0;
                if (i == 0) {

                    switch (dockerContainer.getAppID()) {
                        case "app0":
                            amount = 5;
                            break;
                        case "app1":
                            amount = 85;
                            break;
                        case "app2":
                            amount = 50;
                            break;
                        case "app3":
                            amount = 0;
                            break;
                    }

                } else if (i == 1) {

                    switch (dockerContainer.getAppID()) {
                        case "app0":
                            amount = 0;
                            break;
                        case "app1":
                            amount = 0;
                            break;
                        case "app2":
                            amount = 50;
                            break;
                        case "app3":
                            amount = 50;
                            break;

                    }
                } else {
                    switch (dockerContainer.getAppID()) {
                        case "app0":
                            amount = 160;
                            break;
                        case "app1":
                            amount = 50;
                            break;
                        case "app2":
                            amount = 50;
                            break;
                        case "app3":
                            amount = 0;
                            break;

                    }

                }
                tau_t += i * DockerPlacementService.SLEEP_TIME;
                PlannedInvocation plannedInvocation = new PlannedInvocation(dockerContainer.getDockerImage().getAppId(),
                        amount, tau_t);

                databaseController.save(plannedInvocation);
            }
        }

    }

    public void solve() throws Exception {

        dockerPlacementService.computePlacement(new Date().getTime());
    }
}
