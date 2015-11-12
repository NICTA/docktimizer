package au.csiro.data61.docktimizer.interfaces;

import au.csiro.data61.docktimizer.exception.*;
import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VirtualMachine;
import com.spotify.docker.client.messages.ContainerInfo;

import java.util.List;
import java.util.Map;

/**

 */
public interface DockerController {

    void initialize();

    /**
     *
     * @param running specified if only currently running container or all should be returned
     * @return a map of VMs and list of Docker containers
     */
    public Map<VirtualMachine, List<DockerContainer>> getDockersPerVM(boolean running);

    List<DockerContainer> getDockers(VirtualMachine virtualMachine) throws Exception, CouldNotGetDockerException;

    DockerContainer startDocker(VirtualMachine virtualMachine, DockerContainer dockerContainer) throws CouldNotStartDockerException;

    boolean stopDocker(VirtualMachine virtualMachine, DockerContainer dockerContainer) throws CouldNotStopDockerException;

    ContainerInfo getDockerInfo(VirtualMachine virtualMachine, DockerContainer dockerContainer) throws ContainerNotFoundException;

    DockerContainer resizeContainer(VirtualMachine virtualMachine,
                                    DockerContainer newDockerContainer) throws CouldResizeDockerException;
}
