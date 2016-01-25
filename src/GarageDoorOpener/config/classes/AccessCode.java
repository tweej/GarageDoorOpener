package GarageDoorOpener.config.classes;

import GarageDoorOpener.DateTimeRangeConstraint;
import GarageDoorOpener.DayOfWeekConstraint;
import GarageDoorOpener.TemporalAccessConstraint;
import GarageDoorOpener.TimeOfDayConstraint;

import javax.xml.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@XmlRootElement(name="code")
public class AccessCode {

    @XmlElement private boolean    enabled;
    @XmlElement private String     owner;
    @XmlElement private String     description;
    @XmlList    private List<Short> gpios;
    @XmlElement private GarageDoorOpener.config.classes.AccessConstraints AccessConstraints = new AccessConstraints();



    @XmlAttribute(name="value") private String code;


    public boolean    getEnabled()     { return enabled; }
    public String     getOwner()       { return owner; }
    public String     getDescription() { return description; }
    public List<Short> getGPIOs()       { return gpios; }
    public String     getCode()        { return code; }
    public List<DayOfWeekConstraint>     getDOWConstraints() { return AccessConstraints.getDOWConstraints(); }
    public List<TimeOfDayConstraint>     getTODConstraints() { return AccessConstraints.getTODConstraints(); }
    public List<DateTimeRangeConstraint> getDTRConstraints() { return AccessConstraints.getDTRConstraints(); }

    public boolean unconstrained(LocalDateTime t) {
        boolean unconstrainedDOW = false, unconstrainedTOD = false, unconstrainedDTR = false;

        if(getDOWConstraints().size() == 0) {unconstrainedDOW = true;}
        else {
            for(TemporalAccessConstraint constraint : getDOWConstraints()) {
                unconstrainedDOW |= constraint.unconstrained(t);
            }
        }

        if(getTODConstraints().size() == 0) {unconstrainedTOD = true;}
        else {
            for(TemporalAccessConstraint constraint : getTODConstraints()) {
                unconstrainedTOD |= constraint.unconstrained(t);
            }
        }

        if(getDTRConstraints().size() == 0) {unconstrainedDTR = true;}
        else {
            for(TemporalAccessConstraint constraint : getDTRConstraints()) {
                unconstrainedDTR |= constraint.unconstrained(t);
            }
        }

        return unconstrainedDOW && unconstrainedTOD && unconstrainedDTR;
    }

    public boolean currentlyUnconstrained() {
        return unconstrained(LocalDateTime.now());
    }

    public final boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof AccessCode)) return false;
        AccessCode other = (AccessCode) o;
        return other.getCode().equals(getCode());
    }

    public int hashCode() { return getCode().hashCode(); }

}
