package au.csiro.data61.docktimizer.models;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PlannedInvocations {
    private List<PlannedInvocation> plannedInvocations;

    public void setPlannedInvocations(List<PlannedInvocation> plannedInvocations) {
        this.plannedInvocations = plannedInvocations;
    }

    public List<PlannedInvocation> getPlannedInvocations() {
        return plannedInvocations;
    }
}
