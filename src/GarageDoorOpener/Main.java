package GarageDoorOpener;

// TODO: Use xjc to generate classes and use JAXB external binding file to specify adapters
// NOTE: I spent several hours trying this and made some progress, but the inability to bind adapter classes to complex
//       types with XJC ultimately renders this approach useless for reducing the amount of code I need to write. Very
//       frustrating.
import GarageDoorOpener.config.classes.AccessCode;
import GarageDoorOpener.config.Config;

// Use JavaMail for SMTP client
import javax.mail.*;
import javax.mail.internet.*;

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


    private static void HandleClient(SSLSocket socket) {

        LocalDateTime now = LocalDateTime.now();

        // Check if this client is being blocked for invalid authorization attempts
        TrackedClient client = clients.get(socket.getInetAddress());
        if (client != null && client.blockExpires.isAfter(now)) // client null if lockout disabled
        {
            System.out.println(now + " Blocked client attempted connection: " +
                socket.getInetAddress().getHostAddress());
            try{ socket.close(); } catch(IOException e) { System.err.println(e.toString()); }
            return;
        }

        // Otherwise, read a code from this client
        String codeStr = getCodeFromClient(socket);
        if(codeStr == null) {
            incrementAuthorizationAttempts(client, socket.getInetAddress());
            try{ socket.close(); } catch(IOException e) { System.err.println(e.toString()); }
            return;
        }

        // Now check if the code is a valid code
        AccessCode code = codes.get(codeStr);
        if(code != null && code.currentlyUnconstrained() && code.getEnabled()) {
            System.out.println(now + " Accepted code " + codeStr);
            if(config.emailEnabled()) {
                sendEmail(
                    config.getEmailSender(),
                    config.getEmailRecipeints(),
                    "Garage Door Open Event",
                    "User " + code.getOwner() + " code accepted at " + LocalDateTime.now());
            }

            clients.remove(socket.getInetAddress());
            try {
                for (Short gpio : code.getGPIOs()) myGPIOs.get(gpio).high();
                Thread.sleep(500); // 0.5 second is more than long enough for my garage door opener
            } catch (InterruptedException e) {
                System.err.println(LocalDateTime.now() + " Interrupted...");
            } finally {
                for (Short gpio : code.getGPIOs()) myGPIOs.get(gpio).low();
            }
        } else if(code == null) {
            System.out.println(now + " Invalid code " + codeStr + " entered from IP address " +
                socket.getInetAddress().getHostAddress() + "!");
            if (config.emailEnabled()) {
                sendEmail(
                    config.getEmailSender(),
                    config.getEmailRecipeints(),
                    "Garage Door Open Event",
                    "Invalid code entered at " + LocalDateTime.now());
            }

            incrementAuthorizationAttempts(client, socket.getInetAddress());
        } else if(!code.getEnabled()) {
            System.out.println(LocalDateTime.now() + " Code " + codeStr + " is valid but currently disabled!");
            if(config.emailEnabled()) {
                sendEmail(
                    config.getEmailSender(),
                    config.getEmailRecipeints(),
                    "Garage Door Open Event",
                    "User " + code.getOwner() + " code disabled at " + LocalDateTime.now());
            }

            clients.remove(socket.getInetAddress());
        } else if(!code.currentlyUnconstrained()) {
            System.out.println(LocalDateTime.now() + " Code " + codeStr + " is valid but currently constrained!");
            if(config.emailEnabled()) {
                sendEmail(
                        config.getEmailSender(),
                        config.getEmailRecipeints(),
                        "Garage Door Open Event",
                        "User " + code.getOwner() + " code constrained at " + LocalDateTime.now());
            }

            clients.remove(socket.getInetAddress());
        }
    }

    private static void ValidateAndInitConfiguration() {
        // Create required GPIO objects from config file information using this controller
        final GpioController gpio = GpioFactory.getInstance();

        // This is a mocked version of above for development and testing on a x86 box without the RPi2 GPIO hardware.
//        final GpioPinDigitalOutput mockPin = mock(GpioPinDigitalOutput.class);
//        when(mockPin.getName()).thenReturn("MockPin");
//        final GpioController gpio = mock(GpioController.class);
//        when(gpio.provisionDigitalOutputPin(any(Pin.class), any(PinState.class))).
//                thenReturn(mockPin);

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
                            System.out.println("Configured " + myPin.getName());
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

        if(config.emailEnabled()) {
            for(String recipient : config.getEmailRecipeints()) {
                if(!recipient.endsWith("@gmail.com")) {
                    System.err.println("Only GMail email recipients are supported." +
                        "See https://support.google.com/a/answer/176600?hl=en for details.");
                }
            }
        }
    }

    private static String getCodeFromClient(SSLSocket c) {
        try {
            // Read the code from the client
            //   First the client should send the number of characters / digits in the code.
            //   Then the client should send the number of characters / digits provided in the previous step.
            //   This prohibits the client from sending an absurdly long code, as would be possible if this were instead
            //   implemented by something like r.readline(). The maximum code length is thus constrained by the return
            //   value of Reader.read(), which is the size of a char. (65535 max) The intent is to limit the ability of
            //   a client to completely deny service to others by sending a continuous stream of data.
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()), 20);
            int nChars = r.read();
            if (nChars == 0) {
                System.out.println("Client gave code length of 0!");
                r.close();
                return null;
            } else if (nChars == -1) {
                System.err.println("Reached end of stream for new client while getting length of code");
                r.close();
                return null;
            }

            char[] clientCode = new char[nChars];
            int nRead = r.read(clientCode, 0, nChars);
            if (nRead == -1) {
                System.err.println("Reached end of stream for new client while reading code");
                r.close();
                return null;
            } else if (nRead != nChars) {
                System.err.println("Number of characters read does not match the code length");
                r.close();
                return null;
            }
            r.close();
            return new String(clientCode);
        } catch(IOException e){
            System.err.println(e.toString());
            return null;
        } finally {
            try{ c.close(); } catch(IOException e) { System.err.println("Error closing client socket"); }
        }
    }

    private static void sendEmail(String from, List<String> recipients, String subject, String message) {
        // Use a new thread for email notification in case of a timeout, so this thread does not block and the
        // user does not observe any latency between successful code entry and door opening.
        Runnable emailTask = () -> {
            try {
                // Set the host SMTP address
                Properties props = new Properties();
                props.put("mail.smtp.host", "aspmx.l.google.com");
                props.put("mail.smtp.connectiontimeout", 3000);
                props.put("mail.smtp.timeout", 3000);

                // Create some properties and get the default Session
                Session session = Session.getDefaultInstance(props, null);
                session.setDebug(false);

                // Create a message
                Message msg = new MimeMessage(session);

                // Set the from and to address
                InternetAddress addressFrom = new InternetAddress(from);
                msg.setFrom(addressFrom);

                for(String recipient : recipients)
                    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

                // Setting the Subject and Content Type
                msg.setSubject(subject);
                msg.setContent(message, "text/plain");
                Transport.send(msg);
            } catch (MessagingException e){
                e.printStackTrace();
            }
        };

        new Thread(emailTask).start();
    }

    private static void incrementAuthorizationAttempts(TrackedClient client, InetAddress address)
    {
        if(config.lockoutEnabled()) {
            LocalDateTime now = LocalDateTime.now();

            if(client == null) {
                clients.put(address, new TrackedClient());
            } else if(++client.attempts == config.getAttempts()) {
                client.blockExpires = now.plusSeconds(config.getBlockDuration());
                client.attempts = 0;
            }
        }
    }

}
