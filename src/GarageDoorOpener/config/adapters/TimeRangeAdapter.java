package GarageDoorOpener.config.adapters;

import GarageDoorOpener.TimeOfDayConstraint;
import GarageDoorOpener.config.classes.TimeRange;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalTime;

public class TimeRangeAdapter extends XmlAdapter<TimeRange, TimeOfDayConstraint> {
    public TimeOfDayConstraint unmarshal(TimeRange tod) throws Exception {
        return new TimeOfDayConstraint(LocalTime.parse(tod.getStart()), LocalTime.parse(tod.getEnd()));
    }

    public TimeRange marshal(TimeOfDayConstraint tod) throws Exception {
        return new TimeRange(tod.getBegin().toString(), tod.getEnd().toString());
    }
}