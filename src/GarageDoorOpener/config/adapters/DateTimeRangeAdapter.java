package GarageDoorOpener.config.adapters;


import GarageDoorOpener.DateTimeRangeConstraint;
import GarageDoorOpener.config.classes.DateTimeRange;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;

public class DateTimeRangeAdapter extends XmlAdapter<DateTimeRange, DateTimeRangeConstraint> {
    public DateTimeRangeConstraint unmarshal(DateTimeRange tod) throws Exception {
        return new DateTimeRangeConstraint(LocalDateTime.parse(tod.getStart()), LocalDateTime.parse(tod.getEnd()));
    }

    public DateTimeRange marshal(DateTimeRangeConstraint tod) throws Exception {
        return new DateTimeRange(tod.getBegin().toString(), tod.getEnd().toString());
    }
}