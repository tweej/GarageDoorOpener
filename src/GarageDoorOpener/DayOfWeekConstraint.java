package GarageDoorOpener;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public class DayOfWeekConstraint extends TemporalAccessConstraint {

    final private DayOfWeek dow;

    public DayOfWeekConstraint(DayOfWeek dow) {
        this.dow = dow;
    }

    @Override
    public boolean unconstrained(LocalDateTime t) {
        return t.getDayOfWeek().equals(dow);
    }

    @Override
    public String toString() {
        return dow.toString();
    }

    public DayOfWeek getDayOfWeek() { return dow; }
}
