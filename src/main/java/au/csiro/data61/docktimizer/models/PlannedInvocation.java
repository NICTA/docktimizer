package au.csiro.data61.docktimizer.models;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

/**
 */
@XmlRootElement(name = "PlannedInvocation")
@Entity
@Table(name = "PlannedInvocation")
public class PlannedInvocation {

    @Id
    @GeneratedValue
    private long id;

    private String appId;
    private Integer amount;
    private long invocationDate;
    private boolean done = false;

    public PlannedInvocation() {
    }

    public PlannedInvocation(String appId, Integer amount, long invocationDate) {
        this.appId = appId;
        this.amount = amount;
        this.invocationDate = invocationDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        id = id;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String dockerImage) {
        this.appId = dockerImage;
    }

    public long getInvocationDate() {
        return invocationDate;
    }

    public void setInvocationDate(long invocationDate) {
        this.invocationDate = invocationDate;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
