package GarageDoorOpener.config.classes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Lockout {
    @XmlElement private int attempts;
    @XmlElement private long block_duration;

    public int getAttempts() { return attempts; }
    public long getBlock_duration() { return block_duration; }
}
