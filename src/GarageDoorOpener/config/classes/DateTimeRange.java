package GarageDoorOpener.config.classes;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class DateTimeRange {

    @XmlAttribute(name = "start", required = true)
    protected String start;
    @XmlAttribute(name = "end", required = true)
    protected String end;

    public DateTimeRange() {}

    public DateTimeRange(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() { return start; }
    public String getEnd() {
        return end;
    }

}