package GarageDoorOpener;

import java.time.LocalDateTime;

public abstract class TemporalAccessConstraint {

    public boolean currentlyUnconstrainted() {
        return unconstrained(LocalDateTime.now());
    }

    abstract public boolean unconstrained(LocalDateTime t);
    abstract public String toString();
}
