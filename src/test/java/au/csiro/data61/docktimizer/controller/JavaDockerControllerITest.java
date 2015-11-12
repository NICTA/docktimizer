package au.csiro.data61.docktimizer.controller;

import au.csiro.data61.docktimizer.AbstractTest;
import au.csiro.data61.docktimizer.exception.*;
import au.csiro.data61.docktimizer.models.DockerConfiguration;
import au.csiro.data61.docktimizer.models.DockerContainer;
import com.spotify.docker.client.messages.ContainerInfo;
import org.junit.*;
import org.junit.runners.MethodSorters;

import javax.naming.ldap.Control;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@Ignore("This test should only be run manually as it uses cloud resources and may produce cost")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JavaDockerControllerITest extends AbstractTest {

    private static JavaDockerController javaDockerController;

    @BeforeClass
    public static void setUp() throws Exception {

        ControllerHandler.DEBUG_MODE = false;

        cloudController = ControllerHandler.getCloudControllerInstance();

        javaDockerController = JavaDockerController.getInstance();
        javaDockerController.initialize();

        javaDockerController.setCloudController(cloudController);

    }

    @AfterClass
    public static void afterClass() {
        ControllerHandler.DEBUG_MODE = true;
    }

    @Test
    public void t01_testGetDockersPerVM() throws Exception, CouldNotGetDockerException {
        List<DockerContainer> dockersPerVM = javaDockerController.getDockers(validVirtualMachine);
        assertThat(dockersPerVM.size(), greaterThanOrEqualTo(0));
    }

    @Test
    public void t02_testStartDocker() throws Exception {
        validDockerContainer = javaDockerController.startDocker(validVirtualMachine, validDockerContainer);
        assertThat(validDockerContainer, is(notNullValue()));
    }

    @Test(expected = CouldNotStartDockerException.class)
    public void t03_testStartDocker_InvalidDockerImage() throws Exception {
        javaDockerController.startDocker(validVirtualMachine, invalidDockerContainer);
    }

    @Test
    public void t04_getDockerInfo() throws ContainerNotFoundException {
        ContainerInfo dockerInfo = javaDockerController.getDockerInfo(validVirtualMachine, validDockerContainer);
        assertThat(dockerInfo, is(notNullValue()));
    }

    @Ignore
    @Test
    public void t05_resizeDocker() throws CouldNotStartDockerException, CouldResizeDockerException {

        DockerConfiguration nweConfig = DockerConfiguration.MICRO_CORE;
        bonomatContainer.setContainerConfiguration(nweConfig);

        bonomatContainer = javaDockerController.resizeContainer(validVirtualMachine, bonomatContainer);
        assertThat(validDockerContainer, is(notNullValue()));
    }

    @Test
    public void t06_stopDocker() throws CouldNotStopDockerException {
        boolean b = javaDockerController.stopDocker(validVirtualMachine, validDockerContainer);
        assertTrue(b);
    }

    @Test(expected = CouldNotStopDockerException.class)
    public void t07_stopDocker_InvalidDockerID() throws CouldNotStopDockerException {
        javaDockerController.stopDocker(invalidVirtualMachine, invalidDockerContainer);
    }

    @Test
    public void t09_testStartDockerBonomat() throws Exception {

        bonomatContainer = javaDockerController.startDocker(validVirtualMachine, bonomatContainer);
        assertThat(bonomatContainer, is(notNullValue()));
    }

}