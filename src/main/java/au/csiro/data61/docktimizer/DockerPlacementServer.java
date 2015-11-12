package au.csiro.data61.docktimizer;

import au.csiro.data61.docktimizer.controller.ControllerHandler;
import au.csiro.data61.docktimizer.controller.MysqlDatabaseController;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import au.csiro.data61.docktimizer.service.DockerPlacementRESTApi;
import au.csiro.data61.docktimizer.testClient.DockerPlacementServiceTest;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 */
public class DockerPlacementServer {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(DockerPlacementServiceTest.class);
    private static boolean SETUP = true;
    private final MysqlDatabaseController instance;
    private HttpServer crunchifyHTTPServer;


    public static void main(String[] args) throws Exception {
        DockerPlacementServer dockerPlacementServer = new DockerPlacementServer();
    }

    public DockerPlacementServer() {
        LOG.info("Starting DockerPlacement's Embedded Jersey HTTPServer...\n");
        instance = MysqlDatabaseController.getInstance();
        instance.initializeAndUpdate(new Date().getTime());

        try {
            start();
            Scanner scanner = new Scanner(System.in);
            DockerPlacementRESTApi api = new DockerPlacementRESTApi();


            ControllerHandler controllerHandler = ControllerHandler.getInstance();
            controllerHandler.initializeAndUpdate(new Date().getTime());
            LOG.info("----------------------------------------------");
            LOG.info("----------------CURRENT MODE-------------------");
            LOG.info("----------------" + MysqlDatabaseController.BASELINE_TYPE + "------------------------");
            LOG.info("-----------Enter 'start' to begin -------------");

            String input = "";
            while (!input.equalsIgnoreCase("start")) {
                input = scanner.nextLine();
            }


            switch (MysqlDatabaseController.BASELINE_TYPE) {
                case "1EACH":
                    controllerHandler.setUpOneForEach();
                    break;
                case "1ALL":
                    controllerHandler.setUpAllOnOne();
                    break;
                default:
                    LOG.info("DEFAULT_MODE"); //chose which evaluation run
//                        controllerHandler.setUpOneForEach();
                    controllerHandler.setUpAllOnOne();
                    break;

            }


            LOG.info("Enter 'stop' to terminate");

            while (!input.equalsIgnoreCase("stop")) {
                input = scanner.nextLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            LOG.info("Could not start API");
            e.printStackTrace();
        } finally {
            LOG.info("Terminating....");
            stop();
            System.exit(1);
        }
    }


    private void start() throws IOException {
        crunchifyHTTPServer = createHttpServer();
        crunchifyHTTPServer.start();
        LOG.info(String.format("\nJersey Application Server started with WADL available at " + "%sapplication.wadl\n", getDockerPlacementURI()));
        LOG.info("Started Embedded Jersey HTTPServer Successfully !!!");
        LOG.info("Getting started, waiting for your <enter>");
    }

    private void stop() {
        crunchifyHTTPServer.stop(1);
    }

    private HttpServer createHttpServer() throws IOException {
        ResourceConfig placementResourceConfig = new PackagesResourceConfig("au.csiro.data61.docktimizer.service");

        return HttpServerFactory.create(getDockerPlacementURI(), placementResourceConfig);
    }

    private URI getDockerPlacementURI() {
        return UriBuilder.fromUri("http://" + dockerPlacementServiceName() + "/").port(8088).build();
    }

    private String dockerPlacementServiceName() {
        String hostName = "localhost";
        return hostName;
    }
}