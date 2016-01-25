package GarageDoorOpener.config.adapters;

import GarageDoorOpener.config.classes.Codes;
import GarageDoorOpener.config.classes.AccessCode;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class CodesAdapter extends XmlAdapter<Codes, Map<String, AccessCode>> {

    @Override
    public Map<String, AccessCode> unmarshal(Codes codes) throws Exception {
        Map<String, AccessCode> map = new LinkedHashMap<>();
        for(AccessCode code : codes.getCodes()) {
            map.put(code.getCode(), code);
        }
        return map;
    }

    @Override
    public Codes marshal(Map<String, AccessCode> map) throws Exception {
        Codes codes = new Codes();
        codes.setCodes(new LinkedList<>(map.values()));
        return codes;
    }

}
