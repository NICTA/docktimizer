package au.csiro.data61.docktimizer.service;


import au.csiro.data61.docktimizer.controller.ControllerHandler;
import au.csiro.data61.docktimizer.controller.MysqlDatabaseController;
import au.csiro.data61.docktimizer.interfaces.DatabaseController;
import au.csiro.data61.docktimizer.models.*;
import au.csiro.data61.docktimizer.placement.DockerPlacementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;


@Path("/")
public class DockerPlacementRESTApi {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(DockerPlacementRESTApi.class);


    private final DatabaseController database;
    private final DockerPlacementService dockerPlacementService;

    public DockerPlacementRESTApi() throws Exception {
        this.database = MysqlDatabaseController.getInstance();
        long tau_t = new Date().getTime();

        this.dockerPlacementService = DockerPlacementService.getInstance();

    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response defaultPath() {

        String result = "Up And Running :)";
        return Response.status(200).entity(result).build();
    }

    @GET
    @Path("/api")
    @Produces(MediaType.TEXT_PLAIN)
    public Response api() {


        dockerPlacementService.computePlacement(new Date().getTime());
        String result = "Computing Placement";
        return Response.status(200).entity(result).build();
    }

    @GET
    @Path("/terminate")
    @Produces(MediaType.TEXT_PLAIN)
    public Response terminate() {

        try {
            dockerPlacementService.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String result = "Closing Up";
        return Response.status(200).entity(result).build();
    }

    @GET
    @Path("/state")
    @Produces(MediaType.APPLICATION_JSON)
    public SystemState systemState() {


        SystemState systemstate = new SystemState();
        systemstate.setTimestamp(new Date().getTime());
        SortedMap<VMType, List<VirtualMachine>> vmMap = database.getVmMap(true);
        List<VirtualMachine> machines = new ArrayList<>();
        for (VMType vmType : vmMap.keySet()) {
            for (VirtualMachine virtualMachine : vmMap.get(vmType)) {
                if (virtualMachine.isRunning()) {
                    machines.add(virtualMachine);
                    systemstate.addVM(virtualMachine);
                }
            }
        }
        return systemstate;
    }

    @GET
    @Path("/restart/{mode}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restart(@PathParam("mode") String mode) {

        if (mode == null || mode.length() <= 0
                && !(mode.equalsIgnoreCase(MysqlDatabaseController.DEFAULT)
                || mode.equalsIgnoreCase(MysqlDatabaseController.ONE_ALL)
                || mode.equalsIgnoreCase(MysqlDatabaseController.ONE_EACH))) {
            String result = "Invalid input";
            return Response.status(300).entity(result).build();
        } else {
            LOG.info("Updated mode: " + mode);
            MysqlDatabaseController.BASELINE_TYPE = mode.toUpperCase();
        }

        ControllerHandler controllerHandler = ControllerHandler.getInstance();
        database.restart();
        database.initializeAndUpdate(new Date().getTime());

        Collection<List<VirtualMachine>> vmSet = controllerHandler.getVMMap(true).values();
        for (List<VirtualMachine> virtualMachines : vmSet) {
            LOG.info("VMs running: " + virtualMachines);
            for (VirtualMachine virtualMachine : virtualMachines) {
                if (virtualMachine.isRunning()) {
                    controllerHandler.terminateVM(virtualMachine);
                }
            }
        }
        controllerHandler.SETUP = true;
        switch (MysqlDatabaseController.BASELINE_TYPE) {
            case "1ALL":
                controllerHandler.setUpAllOnOne();
                break;
            case "DEFAULT":
                controllerHandler.setUpAllOnOne();
                break;
            default:
                controllerHandler.setUpOneForEach();
                break;
        }
        controllerHandler.updateExistingServers();

        String result = "Restart done";
        return Response.status(200).entity(result).build();
    }

    @POST
    @Path("/addInvocations")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public Response add(PlannedInvocations invocations) {
        List<PlannedInvocation> databaseInvocations = database.getInvocations();
        for (PlannedInvocation databaseInvocation : databaseInvocations) {
            databaseInvocation.setDone(true);
            database.save(databaseInvocation);
        }

        for (PlannedInvocation plannedInvocation : invocations.getPlannedInvocations()) {
            LOG.info("Received " + plannedInvocation.getAmount() + "planned invocation for " + plannedInvocation.getAppId());
            database.save(plannedInvocation);
        }
        String result = "Invocations Stored";
        return Response.status(200).entity(result).build();
    }

}