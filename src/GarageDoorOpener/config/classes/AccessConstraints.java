package GarageDoorOpener.config.classes;

import GarageDoorOpener.DateTimeRangeConstraint;
import GarageDoorOpener.DayOfWeekConstraint;
import GarageDoorOpener.TimeOfDayConstraint;
import GarageDoorOpener.config.adapters.DayOfWeekAdapter;
import GarageDoorOpener.config.adapters.DateTimeRangeAdapter;
import GarageDoorOpener.config.adapters.TimeRangeAdapter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="AccessConstraints")
public class AccessConstraints {

    @XmlElementWrapper(name="DayOfWeek") @XmlElement(name = "Day") @XmlJavaTypeAdapter(DayOfWeekAdapter.class)
    private List<DayOfWeekConstraint> dowConstraints = new ArrayList<>();

    @XmlElementWrapper(name="TimeOfDay") @XmlElement(name = "TimeRange")  @XmlJavaTypeAdapter(TimeRangeAdapter.class)
    private List<TimeOfDayConstraint> todConstraints = new ArrayList<>();

    @XmlElementWrapper(name="DateTime") @XmlElement(name = "DateTimeRange")  @XmlJavaTypeAdapter(DateTimeRangeAdapter.class)
    private List<DateTimeRangeConstraint> dtrConstraints = new ArrayList<>();

    public List<DayOfWeekConstraint> getDOWConstraints() { return dowConstraints; }

    public List<TimeOfDayConstraint> getTODConstraints() { return todConstraints; }

    public List<DateTimeRangeConstraint> getDTRConstraints() { return dtrConstraints; }

}
