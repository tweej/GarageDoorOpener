package GarageDoorOpener.config.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.net.InetAddress;

public class IPAdapter extends XmlAdapter<String, InetAddress> {
    public InetAddress unmarshal(String ip) throws Exception {
        return InetAddress.getByName(ip);
    }

    public String marshal(InetAddress ip) throws Exception {
        return ip.getHostAddress();
    }
}
