package GarageDoorOpener;

// TODO: Use xjc to generate classes and use JAXB external binding file to specify adapters
// NOTE: I spent several hours trying this and made some progress, but the inability to bind adapter classes to complex
//       types with XJC ultimately renders this approach useless for reducing the amount of code I need to write. Very
//       frustrating.
import GarageDoorOpener.config.classes.AccessCode;
import GarageDoorOpener.config.Config;

import javax.net.ssl.*;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.util.*;

import com.pi4j.io.gpio.*;

// For development / testing on x86 without the RPi2 GPIO hardware.
//import static org.mockito.Mockito.*;

public class Main {

    private static Map<Short, GpioPinDigitalOutput> myGPIOs = new HashMap<>();
    private static Map<String, AccessCode> codes;
    private static HashMap<InetAddress, TrackedClient> clients = new HashMap<>();
    private static Config config;

    public static void main(String[] args) throws JAXBException, org.xml.sax.SAXException {

        JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        // Enforce schema restrictions when unmarshalling
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        jaxbUnmarshaller.setSchema(sf.newSchema(new File("config.xsd")));

        // Use default validation handler to terminate on first unmarshalling failure
        jaxbUnmarshaller.setEventHandler(new DefaultValidationEventHandler());

        config = (Config) jaxbUnmarshaller.unmarshal(new File("config.xml"));
        codes = config.getCodes();

        // Ensure all provided configuration information is usable and makes sense
        ValidateAndInitConfiguration();


        // Set up socket and listen (single-threaded)
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            // Keystore password is pointless: https://gist.github.com/zach-klippenstein/4631307
            ks.load(new FileInputStream(config.getKeystorePath()), "pointlesspassword".toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, config.getPrivateKeyPassword().toCharArray());

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), null, null);
            SSLServerSocketFactory ssf = sc.getServerSocketFactory();
            SSLServerSocket s = (SSLServerSocket) ssf.createServerSocket(config.getPort());
            System.out.println("Listening on: " + config.getIp().getHostAddress() + ":" + config.getPort());
            // Cipher suites are constrained by GarageDoorOpener.properties file

            while(true) {
                SSLSocket c = (SSLSocket) s.accept();
                HandleClient(c);
            }

        } catch(FileNotFoundException e) {
            System.err.println("Could not find keystore file!");
        } catch(KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException |
                KeyManagementException e) {
            System.err.println(e.toString());
        } catch(UnrecoverableKeyException e) {
            System.err.println("Incorrect keypassword provided in config.xml");
        } finally {
            System.exit(1);
        }
    }


    private static void HandleClient(SSLSocket c) {
        try {
            LocalDateTime now = LocalDateTime.now();

            TrackedClient client = clients.get(c.getInetAddress());
            if(client != null && client.blockExpires.isAfter(now)) // client null if lockout disabled
            {
                System.out.println(now + " Blocked client attempted connection: " +
                                   c.getInetAddress().getHostAddress());
                return; // finally closes reader and socket
            }


            // Read the code from the client
            //   First the client should send the number of characters / digits in the code.
            //   Then the client should send the number of characters / digits provided in the previous step.
            //   This prohibits the client from sending an absurdly long code, as would be possible if this were instead
            //   implemented by something like r.readline(). The maximum code length is thus constrained by the return
            //   value of Reader.read(), which is the size of a char. (65535 max) The intent is to limit the ability of
            //   a client to completely deny service to others by sending a continuous stream of data.
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()), 20);
            int nChars = r.read();
            if(nChars == 0) {
                System.out.println("Client gave code length of 0!");
                return; // finally closes reader and socket
            } else if(nChars == -1) {
                System.err.println("Reached end of stream for new client while getting length of code");
                return; // finally closes reader and socket
            }

            char[] clientCode = new char[nChars];
            int nRead = r.read(clientCode, 0, nChars);
            if(nRead == -1) {
                System.err.println("Reached end of stream for new client while reading code");
                return; // finally closes reader and socket
            } else if(nRead != nChars) {
                System.err.println("Number of characters read does not match the code length");
                return; // finally closes reader and socket
            }

            String clientCodeStr = new String(clientCode);

            try {
                AccessCode code = codes.get(String.valueOf(clientCode));

                if(!code.currentlyUnconstrained()) {
                    System.out.println(LocalDateTime.now() + " Code " + clientCodeStr + " is valid but currently constrained!");
                    clients.remove(c.getInetAddress());
                    return; // finally closes reader and socket
                }

                System.out.println(now + " Accepted code " + clientCodeStr);
                clients.remove(c.getInetAddress()); client = null;

                try {
                    for (Short gpio : code.getGPIOs()) myGPIOs.get(gpio).high();
                    Thread.sleep(500); // 0.5 second
                } catch (InterruptedException e) {
                    System.err.println(LocalDateTime.now() + " Interrupted...");
                } finally {
                    for (Short gpio : code.getGPIOs()) myGPIOs.get(gpio).low();
                }

            } catch (NullPointerException e) {
                System.out.println(now + " Invalid code " + clientCodeStr + " entered from IP address " +
                                   c.getInetAddress().getHostAddress() + "!");

                if(config.lockoutEnabled()) {
                    if(client == null) {
                        clients.put(c.getInetAddress(), new TrackedClient());
                    } else if(++client.attempts == config.getAttempts()) {
                        client.blockExpires = now.plusSeconds(config.getBlockDuration());
                        client.attempts = 0;
                    }
                }
            }  finally {
                r.close();
                c.close();
            }
        } catch(IOException e){
            System.err.println(e.toString());
        } finally {
            try{ c.close(); } catch(IOException e) { System.err.println("Error closing client socket"); }
        }
    }

    private static void ValidateAndInitConfiguration() {
        // Create required GPIO objects from config file information using this controller
        final GpioController gpio = GpioFactory.getInstance();

        // This is a mocked version of above for development and testing on a x86 box without the RPi2 GPIO hardware.
//        final GpioController gpio = mock(GpioController.class);
//        when(gpio.provisionDigitalOutputPin(any(Pin.class), any(PinState.class))).
//                thenReturn(mock(GpioPinDigitalOutput.class));

        // Iterate through the codes only once. In this iteration we will:
        //   Determine if no codes are enabled (in which case the program will terminate after the loop)
        //   Create and store GPIO Pin objects for each GPIO used by any enabled code (if not already created)
        //   Ensure start times are at or before end times for TimeOfDayConstraints and DateTimeRangeConstraints
        //     NOTE : Need XML Schema 1.1's assert to obviate code for this, which no version of JAXB yet supports
        //     https://java.net/jira/browse/JAXB-994
        //   Ensure time ranges and date-time ranges for TimeOfDayConstraints and DateTimeRangeConstraints do not
        //     overlap
        boolean enabled = false;
        for(AccessCode code : codes.values()) {
            if(code.getEnabled()) {
                enabled = true;

                for (Short gpioNum : code.getGPIOs()) {
                    if (!myGPIOs.containsKey(gpioNum)) {
                        try {
                            GpioPinDigitalOutput myPin = gpio.provisionDigitalOutputPin(
                                    RaspiPin.getPinByName("GPIO " + gpioNum.toString()),
                                    PinState.LOW);
                            myPin.setShutdownOptions(true); // Unexport the pin on termination
                            myGPIOs.put(gpioNum, myPin);
                            System.out.println("Configured " + myPin.getPin().getName());
                        } catch(NullPointerException e) {
                            System.err.println("Invalid GPIO (" + gpioNum + ") specified in config.xml");
                            System.exit(1);
                        }
                    }
                }

                for(int i=0; i<code.getTODConstraints().size(); ++i) {
                    TimeOfDayConstraint constraint = code.getTODConstraints().get(i);
                    if (constraint.getBegin().isAfter(constraint.getEnd())) {
                        System.err.println("TimeOfDay constraint beginning (" + constraint.getBegin().toString() +
                                ") is after end( " + constraint.getEnd().toString() + "!");
                        System.exit(1);
                    }

                    for(int j=i-1; j>=0; --j) {
                        TimeOfDayConstraint other = code.getTODConstraints().get(j);
                        if((constraint.getBegin().isAfter(other.getBegin()) &&
                            constraint.getBegin().isBefore(other.getEnd())) ||
                           (constraint.getEnd().isAfter(other.getBegin()) &&
                            constraint.getEnd().isBefore(other.getEnd())) ||
                           (constraint.getBegin().isBefore(other.getBegin()) &&
                            constraint.getEnd().isAfter(other.getEnd()))) {

                            System.err.println("TimeOfDay constraint " + constraint +
                                               " overlaps with constraint " + other);
                            System.exit(1);
                        }
                    }
                }

                for(int i=0; i<code.getDTRConstraints().size(); ++i) {
                    DateTimeRangeConstraint constraint = code.getDTRConstraints().get(i);
                    if (constraint.getBegin().isAfter(constraint.getEnd())) {
                        System.err.println("DateTime constraint beginning (" + constraint.getBegin().toString() +
                                ") is after end( " + constraint.getEnd().toString() + "!");
                        System.exit(1);
                    }

                    for(int j=i-1; j>=0; --j) {
                        DateTimeRangeConstraint other = code.getDTRConstraints().get(j);
                        if((constraint.getBegin().isAfter(other.getBegin()) &&
                            constraint.getBegin().isBefore(other.getEnd())) ||
                           (constraint.getEnd().isAfter(other.getBegin()) &&
                            constraint.getEnd().isBefore(other.getEnd())) ||
                           (constraint.getBegin().isBefore(other.getBegin()) &&
                            constraint.getEnd().isAfter(other.getEnd()))) {
                            System.err.println("DateTime constraint " + constraint +
                                               " overlaps with constraint " + other);
                            System.exit(1);
                        }
                    }
                }
            }
        }
        if(!enabled) {
            System.err.println("There are no enabled codes in the configuration file!");
            System.exit(1);
        }
    }

}
