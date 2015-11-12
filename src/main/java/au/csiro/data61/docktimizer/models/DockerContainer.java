package au.csiro.data61.docktimizer.models;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 */

@Entity
@Table(name = "DockerContainer")
public class DockerContainer {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private DockerConfiguration containerConfiguration;

    @ManyToOne
    private DockerImage dockerImage;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<VirtualMachine> virtualMachines;

    private int amountOfPossibleInvocations;

    private long executionTime;
    private long deployTime;
    private long deployCost = 3;
    private String containerID;

    public DockerContainer() {
    }

    public DockerContainer(DockerImage dockerImage, DockerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
        this.dockerImage = dockerImage;
        this.executionTime = 3000;
        this.amountOfPossibleInvocations = getInvocationAmount(dockerImage.getAppId());
        this.deployTime = 30000;
    }


    public DockerContainer(DockerImage dockerImag, long executionTime, DockerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
        this.executionTime = executionTime;

        this.amountOfPossibleInvocations = getInvocationAmount(dockerImag.getAppId());
        this.deployTime = 30000;
        this.dockerImage = dockerImag;
    }

    private int getInvocationAmount(String appId) {
        int perCore = 0;
        switch ((int) containerConfiguration.cores) {
            case 1:
                perCore = (int) Math.round(containerConfiguration.cores * 80);
                break;
            case 2:
                perCore = (int) Math.round(containerConfiguration.cores * 90);
                break;
            case 4:
                perCore = (int) Math.round(containerConfiguration.cores * 100);
                break;
            case 8:
                perCore = (int) Math.round(containerConfiguration.cores * 110);
                break;
            default:
                perCore = (int) Math.round(containerConfiguration.cores * 100);
                break;
        }
        switch (appId) {
            case "app0":
                return (int) (perCore * 1.0);
            case "app1":
                return (int) (perCore * 1.0);
            case "app2":
                return (int) (perCore * 0.6);
            case "app3":
                return (int) (perCore * 1.0);
            default:
                return (int) (perCore * 1.0);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerContainer)) return false;

        DockerContainer that = (DockerContainer) o;

        return this.dockerImage.getAppId().equals(that.getAppID());

    }

    @Override
    public int hashCode() {
        return this.dockerImage.getAppId().hashCode();
    }

    public String getName() {
        return containerConfiguration.name() + "_" + this.dockerImage.getAppId();
    }

    public DockerConfiguration getContainerConfiguration() {
        return containerConfiguration;
    }

    public String getAppID() {
        return this.dockerImage.getAppId();
    }

    public int getAmountOfPossibleInvocations() {
        return amountOfPossibleInvocations;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public long getDeployTime() {
        return deployTime;
    }

    public long getDeployCost() {
        return deployCost;
    }

    public DockerImage getDockerImage() {
        return dockerImage;
    }

    public String getContainerID() {
        return containerID;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setContainerConfiguration(DockerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
    }

    public void setDockerImage(DockerImage dockerImage) {
        this.dockerImage = dockerImage;
    }

    public void setAmountOfPossibleInvocations(int amountOfPossibleInvocations) {
        this.amountOfPossibleInvocations = amountOfPossibleInvocations;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public void setDeployTime(long deployTime) {
        this.deployTime = deployTime;
    }

    public void setDeployCost(long deployCost) {
        this.deployCost = deployCost;
    }

    public void setContainerID(String containerID) {
        this.containerID = containerID;
    }

    public List<VirtualMachine> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(List<VirtualMachine> virtualMachine) {
        this.virtualMachines = virtualMachine;
    }

    public void addVirtualMachine(VirtualMachine virtualMachine) {
        if (virtualMachines == null) {
            virtualMachines = new ArrayList<>();
        }
        virtualMachines.add(virtualMachine);

    }
}
