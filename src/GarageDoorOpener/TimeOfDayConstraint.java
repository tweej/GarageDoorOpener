package GarageDoorOpener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class TimeOfDayConstraint extends TemporalAccessConstraint {

    final private LocalTime begin, end;

    public TimeOfDayConstraint(LocalTime begin, LocalTime end) {
        this.begin = begin.truncatedTo(ChronoUnit.SECONDS);
        this.end   = end.truncatedTo(ChronoUnit.SECONDS);
    }

    @Override
    public boolean unconstrained(LocalDateTime t) {
        return (t.toLocalTime().isAfter(begin) && t.toLocalTime().isBefore(end)) ||
                t.toLocalTime().equals(begin)  || t.toLocalTime().equals(end);
    }

    @Override
    public String toString() {
        return "[" + begin.toString() + "," + end.toString() + "]";
    }

    public LocalTime getBegin() { return begin; }
    public LocalTime getEnd() { return end; }
}
