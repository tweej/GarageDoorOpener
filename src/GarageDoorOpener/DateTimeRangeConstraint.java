package GarageDoorOpener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateTimeRangeConstraint extends TemporalAccessConstraint {

    final private LocalDateTime begin, end;

    public DateTimeRangeConstraint(LocalDateTime begin, LocalDateTime end) {
        this.begin = begin.truncatedTo(ChronoUnit.SECONDS);
        this.end   = end.truncatedTo(ChronoUnit.SECONDS);
    }

    @Override
    public boolean unconstrained(LocalDateTime t) {
        return (t.isAfter(begin) && t.isBefore(end)) ||
                t.equals(begin)  || t.equals(end);
    }

    @Override
    public String toString() {
        return "[" + begin.toString() + "," + end.toString() + "]";
    }

    public LocalDateTime getBegin() { return begin; }
    public LocalDateTime getEnd()   { return end; }
}
