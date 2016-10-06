package GarageDoorOpener.config;

import GarageDoorOpener.config.adapters.CodesAdapter;
import GarageDoorOpener.config.adapters.IPAdapter;
import GarageDoorOpener.config.classes.AccessCode;
import GarageDoorOpener.config.classes.Email;
import GarageDoorOpener.config.classes.Lockout;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;



@XmlRootElement
public class Config {

    @XmlElement private int listen_port;
    @XmlElement @XmlJavaTypeAdapter(IPAdapter.class) private InetAddress listen_ip;
    @XmlElement private String keystore;
    @XmlElement private String keypassword;
    @XmlElement private Lockout lockout;
    @XmlElement private Email email;

    @XmlElement(name="codes") @XmlJavaTypeAdapter(CodesAdapter.class) private Map<String, AccessCode> codes;


    public boolean emailEnabled()             { return email != null; }
    public int getAttempts()                  { return lockout.getAttempts(); }
    public long getBlockDuration()            { return lockout.getBlock_duration(); }
    public Map<String, AccessCode> getCodes() { return codes; }
    public String getEmailSender()            { return email.getSender(); }
    public List<String> getEmailRecipeints()  { return email.getRecipients(); }
    public InetAddress getIp()                { return listen_ip; }
    public String getKeystorePath()           { return keystore; }
    public int getPort()                      { return listen_port; }
    public String getPrivateKeyPassword()     { return keypassword; }
    public boolean lockoutEnabled()           { return lockout != null; }
}
