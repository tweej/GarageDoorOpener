package GarageDoorOpener.config.classes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class Email {
    @XmlElement private String sender;

    @XmlElementWrapper(name="recipients") @XmlElement(name = "recipient")
    private List<String> recipients = new ArrayList<>();

    public String getSender()           { return sender; }
    public List<String> getRecipients() { return recipients; }
}
