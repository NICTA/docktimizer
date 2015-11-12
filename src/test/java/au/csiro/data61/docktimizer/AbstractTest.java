package au.csiro.data61.docktimizer;

import au.csiro.data61.docktimizer.controller.ControllerHandler;
import au.csiro.data61.docktimizer.controller.MysqlDatabaseController;
import au.csiro.data61.docktimizer.controller.OpenstackCloudController;
import au.csiro.data61.docktimizer.interfaces.CloudController;
import au.csiro.data61.docktimizer.models.*;
import org.junit.BeforeClass;

import static org.mockito.Mockito.mock;

/**
 */
public abstract class AbstractTest {

    protected static DockerContainer invalidDockerContainer;
    protected static DockerContainer validDockerContainer;
    protected static VirtualMachine validVirtualMachine;
    protected static VirtualMachine invalidVirtualMachine;
    protected static DockerContainer joomlaDockerContainer;
    protected static CloudController cloudController;
    protected static DockerContainer bonomatContainer;
    protected static DockerContainer zeroToWordpress;

    @BeforeClass
    public static void setup() {
        ControllerHandler.DEBUG_MODE = true;
        MysqlDatabaseController.BASELINE_TYPE = MysqlDatabaseController.DEFAULT;
        validVirtualMachine = new VirtualMachine("Manager", VMType.M1_MICRO, 9, "localhost"); //replace this IP if you really want to run IT tests
        invalidVirtualMachine = new VirtualMachine("err", VMType.M1_MICRO, 9, "999.999.999.999");

        invalidDockerContainer = new DockerContainer(new DockerImage("app1", "invalid", "image", 8081, 80), DockerConfiguration.MICRO_CORE);
        validDockerContainer = new DockerContainer(MysqlDatabaseController.parseByAppId("app0"), DockerConfiguration.MICRO_CORE);
        joomlaDockerContainer = new DockerContainer(MysqlDatabaseController.parseByAppId("app1"), DockerConfiguration.MICRO_CORE);
        zeroToWordpress = new DockerContainer(MysqlDatabaseController.parseByAppId("app2"), DockerConfiguration.SINGLE_CORE);
        bonomatContainer = new DockerContainer(MysqlDatabaseController.parseByAppId("app3"), DockerConfiguration.MICRO_CORE);

        if (ControllerHandler.DEBUG_MODE) {
            cloudController = mock(OpenstackCloudController.class);
        }
    }

}
