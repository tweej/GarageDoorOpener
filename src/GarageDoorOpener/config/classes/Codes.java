package GarageDoorOpener.config.classes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name="codes")
public class Codes {
    private List<AccessCode> codes;

    @XmlElement(name="code")
    public List<AccessCode> getCodes() {
        return codes;
    }

    public void setCodes(List<AccessCode> codes) {
        this.codes = codes;
    }
}
