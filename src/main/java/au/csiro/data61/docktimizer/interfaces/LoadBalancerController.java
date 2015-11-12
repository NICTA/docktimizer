package au.csiro.data61.docktimizer.interfaces;

import au.csiro.data61.docktimizer.models.DockerContainer;
import au.csiro.data61.docktimizer.models.VirtualMachine;

import java.util.List;
import java.util.Map;

/**
 */
public interface LoadBalancerController {

    /**
     * initializes LoadBalancer
     */
    void initialize();

    /**
     * update HAPRoxy configuration (i.e., the used LoadBalancer)
     * @param virtualMachines containing Docker containers
     * @return status code of update, if failed -1,
     */
    int updateHAProxyConfiguration(List<VirtualMachine> virtualMachines);
}
