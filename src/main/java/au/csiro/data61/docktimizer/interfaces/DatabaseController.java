package au.csiro.data61.docktimizer.interfaces;

import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.PlannedInvocation;
import au.csiro.data61.docktimizer.models.VMType;
import au.csiro.data61.docktimizer.models.VirtualMachine;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 */
public interface DatabaseController {
    /**
     * initializes the database controller and creates the required models in the db
     * @param tau_t = time in milliseconds
     */
    void initializeAndUpdate(long tau_t);

    /**
     *
     * @return a map of dockers
     *  key: docker type (e.g., by app)
     *  value: a list of docker configurations
     */
    Map<DockerContainer, List<DockerContainer>> getDockerMap();

    /**
     *
     * @param update true if database should be updated or not
     * @return a map of VMs
     *  key: VM type
     *  value: a list of VMs
     */
    SortedMap<VMType, List<VirtualMachine>> getVmMap(boolean update);

    /**
     *
     * @param dockerContainerType to find invocations
     * @return amount of upcomming invocations for defined type
     */
    int getInvocations(DockerContainer dockerContainerType);


    /**
     *
     * @param plannedInvocation to be stored in database
     */
    void save(PlannedInvocation plannedInvocation);

    /**
     *
     * @param virtualMachine to be updated in the database
     */
    void update(VirtualMachine virtualMachine);

    /**
     *
     * @return a list of planned invocations
     */
    List<PlannedInvocation> getInvocations();

    /**
     *
     * @param appID defines a specific App ID
     * @return a docker configuration according the given App ID
     */
    DockerContainer getDocker(String appID);

    /**
     * method to reset all configurations
     */
    void restart();

    /**
     * closes database connection
     */
    void close();
}
