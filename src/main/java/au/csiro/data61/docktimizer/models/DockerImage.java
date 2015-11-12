package au.csiro.data61.docktimizer.models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 */
@XmlRootElement(name = "DockerImage")
@Entity
@Table(name = "DockerImage")
public class DockerImage {

    public DockerImage() {
    }

    public DockerImage(String appId, String repoName, String imageName, Integer externPort, Integer internPort) {
        this.appId = appId;
        this.repoName = repoName;
        this.imageName = imageName;
        this.internPort = internPort;
        this.externPort = externPort;
    }

    @Id
    private String appId;

    private String imageName;
    private String repoName;
    private Integer internPort;
    private Integer externPort;


    public String getImageName() {
        return imageName;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getFullName() {
        return String.format("%s/%s", repoName, imageName);
    }

    public Integer getInternPort() {
        return internPort;
    }

    public void setInternPort(Integer internPort) {
        this.internPort = internPort;
    }

    public Integer getExternPort() {
        return externPort;
    }

    public void setExternPort(Integer externPort) {
        this.externPort = externPort;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }


}
