package GarageDoorOpener.config.adapters;

import GarageDoorOpener.DayOfWeekConstraint;
import GarageDoorOpener.config.classes.DayOfWeekEnum;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.DayOfWeek;

public class DayOfWeekAdapter extends XmlAdapter<DayOfWeekEnum, DayOfWeekConstraint> {
    public DayOfWeekConstraint unmarshal(DayOfWeekEnum dow) throws Exception {
        switch(dow) {
            case SUNDAY:
                return new DayOfWeekConstraint(DayOfWeek.SUNDAY);
            case MONDAY:
                return new DayOfWeekConstraint(DayOfWeek.MONDAY);
            case TUESDAY:
                return new DayOfWeekConstraint(DayOfWeek.TUESDAY);
            case WEDNESDAY:
                return new DayOfWeekConstraint(DayOfWeek.WEDNESDAY);
            case THURSDAY:
                return new DayOfWeekConstraint(DayOfWeek.THURSDAY);
            case FRIDAY:
                return new DayOfWeekConstraint(DayOfWeek.FRIDAY);
            case SATURDAY:
                return new DayOfWeekConstraint(DayOfWeek.SATURDAY);
        }
        throw new IllegalArgumentException(dow.toString());
    }

    public DayOfWeekEnum marshal(DayOfWeekConstraint dow) throws Exception {
        switch(dow.getDayOfWeek()) {
            case SUNDAY:
                return DayOfWeekEnum.SUNDAY;
            case MONDAY:
                return DayOfWeekEnum.MONDAY;
            case TUESDAY:
                return DayOfWeekEnum.TUESDAY;
            case WEDNESDAY:
                return DayOfWeekEnum.WEDNESDAY;
            case THURSDAY:
                return DayOfWeekEnum.THURSDAY;
            case FRIDAY:
                return DayOfWeekEnum.FRIDAY;
            case SATURDAY:
                return DayOfWeekEnum.SATURDAY;
        }
        throw new IllegalArgumentException(dow.toString());
    }
}
