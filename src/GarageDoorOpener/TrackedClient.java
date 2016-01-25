package GarageDoorOpener;

import java.time.LocalDateTime;

public class TrackedClient {
    public int attempts = 1;
    public LocalDateTime blockExpires = LocalDateTime.MIN;
}
