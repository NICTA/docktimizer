package au.csiro.data61.docktimizer.models;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 */

@Entity
@Table(name = "VirtualMachine")
public class VirtualMachine {

    @Enumerated(EnumType.STRING)
    private VMType vmType;

    private Integer position; //i.e., the k-th

    private double freeCPU;
    private double freeRAM;
    private boolean isRunning;
    private long startDate;
    private long toBeTerminatedAt;

    @Id
    private String id; // this is the internal ID of the Virtual Machine, only used in connection with the Cloud provider

    private String ip; // this is the IP of the Virtual Machine

    @ManyToMany(mappedBy = "virtualMachines")
    private List<DockerContainer> deployedContainers;
    private boolean terminatable = true;

    public VirtualMachine() {
    }

    public VirtualMachine(String id, VMType vmType, int position, String ip) {
        this.id = id;
        this.vmType = vmType;
        this.position = position;
        this.ip = ip;
    }

    public VirtualMachine(String id, VMType vmType, int position) {
        this.id = id;
        this.vmType = vmType;
        this.position = position;
    }


    public VMType getVmType() {
        return vmType;
    }

    public void setVmType(VMType vmType) {
        this.vmType = vmType;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getName() {
        return position + "_" + vmType.getReadableString();
    }

    public double getFreeCPU() {
        return freeCPU;
    }

    public void setFreeCPU(double freeCPU) {
        this.freeCPU = freeCPU;
    }

    public double getFreeRAM() {
        return freeRAM;
    }

    public void setFreeRAM(double freeRAM) {
        this.freeRAM = freeRAM;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public boolean isDockerDeployed(DockerContainer dockerContainer) {
        if (deployedContainers == null) {
            deployedContainers = new ArrayList<>();
        }
        return deployedContainers.contains(dockerContainer);
    }

    public List<DockerContainer> getDeployedContainers() {
        if (deployedContainers == null) {
            deployedContainers = new ArrayList<>();
        }
        return deployedContainers;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void addDockerContainer(DockerContainer dockerContainer) {
        if (deployedContainers == null) {
            this.deployedContainers = new ArrayList<>();
        }
        if (!deployedContainers.contains(dockerContainer)) {
            deployedContainers.add(dockerContainer);
        }
        dockerContainer.addVirtualMachine(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirtualMachine)) return false;

        VirtualMachine that = (VirtualMachine) o;

        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }

    public void setDeplyoedContainers(List<DockerContainer> containers) {
        this.deployedContainers = containers;
    }


    public boolean isTerminatable() {
        return terminatable;
    }

    public void setTerminatable(boolean terminatable) {
        this.terminatable = terminatable;
    }

    public long getToBeTerminatedAt() {
        return toBeTerminatedAt;
    }

    public void setToBeTerminatedAt(long toBeTerminatedAt) {
        this.toBeTerminatedAt = toBeTerminatedAt;
    }
}
