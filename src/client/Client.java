package client;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.StatusMessage;
import common.protocol.user_creation.CreateMessage;
import common.protocol.user_creation.UserBuilder;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.codec.Base32;
import merrimackutil.json.types.JSONObject;
import merrimackutil.util.Tuple;

public class Client {
    private static ProtocolChannel channel = null;
    private static String user;
    private static String host;
    private static int port;
    private static boolean create = false;
    private static boolean post = false;
    private static boolean get = false;
    private static String recvr;
    private static String message;
    private static String privKey;
    public static void usage() {
        System.out.println("usage:");
        System.out.println("  client --create --user <user> --host <host> --port <portnum>");
        System.out.println("  client --post <msg> --user <user> --recvr <user> --host <host> --port <portnum>");
        System.out.println("  client --get --key <privkey> --user <user> --host <host> --port <portnum>");
        System.out.println("options:");
        System.out.println("  -c, --create     Create a new account.");
        System.out.println("  -o, --post       Post a message.");
        System.out.println("  -g, --get        Get all posts.");
        System.out.println("  -r, --recvr      The message receiver.");
        System.out.println("  -k, --key        The private key.");
        System.out.println("  -u, --user       The username.");
        System.out.println("  -h, --host       The host name of the server.");
        System.out.println("  -p, --port       The port number for the server.");
        System.exit(1);
    }

    public static void processArgs(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
        }

        OptionParser parser;
        LongOption[] opts = new LongOption[8];
        opts[0] = new LongOption("create", false, 'c');
        opts[1] = new LongOption("post", true, 'o');
        opts[2] = new LongOption("get", false, 'g');
        opts[3] = new LongOption("recvr", true, 'r');
        opts[4] = new LongOption("key", true, 'k');
        opts[5] = new LongOption("user", true, 'u');
        opts[6] = new LongOption("host", true, 'h');
        opts[7] = new LongOption("port", true, 'p');

        parser = new OptionParser(args);
        parser.setLongOpts(opts);
        parser.setOptString("cgo:r:k:u:h:p:");

        Tuple<Character, String> currOpt;

        while (parser.getOptIdx() != args.length) {
            currOpt = parser.getLongOpt(false);

            switch (currOpt.getFirst()) {
                case 'c': create = true; break;
                case 'o': post = true; message = currOpt.getSecond(); break;
                case 'g': get = true; break;
                case 'r': recvr = currOpt.getSecond(); break;
                case 'k': privKey = currOpt.getSecond(); break;
                case 'u': user = currOpt.getSecond(); break;
                case 'h': host = currOpt.getSecond(); break;
                case 'p':
                    try {
                        port = Integer.parseInt(currOpt.getSecond());
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid port number.");
                        usage();
                    }
                    break;
                case '?':
                default: usage(); break;
            }
        }

        // Validate and dispatch
        if (create) {
            if (user == null || host == null || port == 0) {
                System.err.println("Error: Missing required arguments for --create.");
                usage();
            }
            System.out.println("Creating account for user: " + user);
            // TODO: Add create logic
        } else if (post) {
            if (user == null || host == null || port == 0 || recvr == null || message == null) {
                System.err.println("Error: Missing required arguments for --post.");
                usage();
            }
            System.out.println("Posting message from " + user + " to " + recvr + ": " + message);
            Socket socket = new Socket(host, port);
             // Instantiate PostClient correctly using the new keyword
             PostClient postClient = new PostClient(socket);
             postClient.sendMessage(recvr, message);
            postClient.sendMessage(recvr, message);
        } else if (get) {
            if (user == null || host == null || port == 0 || privKey == null) {
                System.err.println("Error: Missing required arguments for --get.");
                usage();
            }
            System.out.println("Retrieving posts for user: " + user);
            // TODO: Add get logic
        } else {
            System.err.println("Error: No valid action specified.");
            usage();
        }
    }


    public static void main(String[] args) throws Exception {
        // Register Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());

        processArgs(args);
        UserBuilder msgBuilder = null;
        
        if (create) {
            System.out.print("Enter a password: ");
            String password = new String(System.console().readPassword());
        
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ElGamal", "BC");
            keyGen.initialize(2048);
            KeyPair kp = keyGen.generateKeyPair();
        
            String pubKeyEncoded = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            String privKeyEncoded = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        
            System.out.println("Public key: " + pubKeyEncoded);
            System.out.println("Private key: " + privKeyEncoded); // Prompt user to save
        
            Socket socket = new Socket(host, port);
            channel = new ProtocolChannel(socket);
        
            JSONObject usermsg = new JSONObject();
            usermsg.put("type", "create");
            usermsg.put("username", user);
            usermsg.put("password", password);
            usermsg.put("publickey", pubKeyEncoded);
           
           System.out.println("Sending create message: " + usermsg.toString());
           Message msg = UserBuilder.buildMessage(usermsg.toString());
         
            //send the user to the server
            channel.sendMessage(msg);
    
            
            // Receive the response
            Message response = channel.receiveMessage();
            System.out.println("Received response: " + response);
            
            // Additional debug info for response handling
            if (response != null) {
                System.out.println("Response class: " + response.getClass().getSimpleName());
                System.out.println("Response content: " + response.toString());
            } else {
                System.out.println("No response received.");
            }
            
            if (!(response instanceof StatusMessage)) {
                System.out.println("Unexpected response from server: " + response.getClass().getSimpleName());
                channel.closeChannel();
                return;
            }
            
        
            StatusMessage status = (StatusMessage) response;
            if (status.getStatus()) {
                System.out.println("Account created successfully.");
                System.out.println("Your private key (SAVE THIS SAFELY!):\n" + privKeyEncoded);
        
                String totpKey = Base32.encodeToString(
                    Base64.getDecoder().decode(status.getPayload().getBytes(StandardCharsets.UTF_8)), false
                );
                System.out.println("TOTP Secret (Base32 for FreeOTP/Google Authenticator):\n" + totpKey);
            } else {
                System.out.println("Failed to create account: " + status.getPayload());
            }
        
            channel.closeChannel();
        }
    }
}
