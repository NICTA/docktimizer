package au.csiro.data61.docktimizer.models;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SystemState {
    long timestamp;
    private Map<String, List<String>> vmContainers;

    public SystemState() {
        vmContainers = new HashMap<>();
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void addVM(VirtualMachine virtualMachine) {
        List<String> containers = new ArrayList<>();
        for (DockerContainer dockerContainer : virtualMachine.getDeployedContainers()) {
            containers.add(dockerContainer.getDockerImage().getAppId() + "-" +
                    dockerContainer.getContainerConfiguration().cores);
        }
        this.vmContainers.put(virtualMachine.getPosition() + "_" + virtualMachine.getVmType().cores, containers);
    }

    public long getTimestamp() {
        return timestamp;
    }


    public Map<String, List<String>> getVmContainers() {
        return vmContainers;
    }
}
