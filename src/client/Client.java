package client;

import java.io.Console;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.bouncycastle.util.Objects;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import common.protocol.Message;
import common.protocol.ProtocolChannel;

import common.protocol.messages.AuthenticateMessage;
import common.protocol.messages.GetMessage;
import common.protocol.messages.GetResponseMessage;
import common.protocol.messages.PostMessage;
import common.protocol.messages.StatusMessage;
import common.protocol.user_creation.CreateMessage;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.codec.Base32;
import merrimackutil.util.NonceCache;
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
    private static NonceCache nonceCache;

    private static final Objects mapper = new Objects();

    /**
     * Prints the help menu.
     */
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

    /**
     * Process the command line arguments.
     * @param args the array of command line arguments.
     * @throws Exception if something goes wrong.
     */
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
            // Create logic runs in main()
        } else if (post) {
            if (user == null || host == null || port == 0 || recvr == null || message == null) {
                System.err.println("Error: Missing required arguments for --post.");
                usage();
            }
            if (!authenticateUser()) {
                System.out.println("Authentication failed.");
                return;
            }
            System.out.println("Authenticated.");
            System.out.println("Posting message from " + user + " to " + recvr + ": " + message);
        
            // TLS and send message
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            socket.startHandshake();
        
            PostClient postClient = new PostClient(socket);
            postClient.sendMessage(user,recvr, message);
        } else if (get) {
            if (user == null || host == null || port == 0 || privKey == null) {
                System.err.println("Error: Missing required arguments for --get.");
                usage();
            }
            if (!authenticateUser()) {
                System.out.println("Authentication failed.");
                return;
            }
            System.out.println("Authenticated.");
            System.out.println("Retrieving posts for user: " + user);
            handleGet();
        } else {
            System.err.println("Error: No valid action specified.");
            usage();
        }
    }

    /**
     * Authenticate a user by prompting for a password and OTP, then
     * performing a TLS handshake with the server, sending an AuthenticateMessage,
     * and receiving a StatusMessage in response. If the status message is
     * success, the function returns true; otherwise, it returns false.
     * @return true if authentication is successful; false otherwise
     * @throws Exception if an error occurs during TLS connection or message
     * processing
     */
    private static boolean authenticateUser() throws Exception {
            Console console = System.console();

    if (console == null) {
        throw new IllegalStateException("Console is not available. Make sure you're running from a terminal.");
    }

    // Hide password input
    char[] passwordChars = console.readPassword("Enter password: ");
    String password = new String(passwordChars);

    // Show OTP input (normal)
    String otp = console.readLine("Enter OTP: ");
      
// Start TLS
try {
   

    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
  
    socket.startHandshake();
   

    // Create protocol channel
    channel = new ProtocolChannel(socket);
    

    // Register message types
    channel.addMessageType(new StatusMessage());
    channel.addMessageType(new AuthenticateMessage());
  

    // Prepare and send AuthenticateMessage
    AuthenticateMessage authMsg = new AuthenticateMessage(user, password, otp);
    

    // Send authentication message
    channel.sendMessage(authMsg);
  

    // Receive response
    Message response = channel.receiveMessage();
  

    // Check the response type
    if (!(response instanceof StatusMessage)) {
        System.out.println("[ERROR] Unexpected response: " + response.getClass().getName());
        return false;
    }

    // Process status message
    StatusMessage status = (StatusMessage) response;
    
    return status.getStatus(); // true = success
} catch (Exception e) {
    e.printStackTrace();
    System.out.println("[ERROR] Exception during TLS connection or message processing: " + e.getMessage());
    return false;
}
    }
    private static void handleGet() throws Exception {
   
    // Load private key from Base64 string
    byte[] privKeyBytes = java.util.Base64.getDecoder().decode(privKey);
    KeyFactory keyFactory = KeyFactory.getInstance("ElGamal", "BC");
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privKeyBytes);
    PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
    
    // Set up TLS + ProtocolChannel
    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
    socket.startHandshake();

    channel = new ProtocolChannel(socket);
    channel.addMessageType(new GetMessage());
    channel.addMessageType(new GetResponseMessage());
    channel.addMessageType(new PostMessage());
    channel.addMessageType(new StatusMessage());

    // Send get message
    channel.sendMessage(new GetMessage(user));

    Message response = channel.receiveMessage();
  

    if (!(response instanceof GetResponseMessage)) {
        System.out.println("Unexpected response from server.");
        return;
    }

    // Process posts
    GetResponseMessage getResp = (GetResponseMessage) response;
    System.out.println("You have " + getResp.getPosts().size() + " message(s):\n");

    for (PostMessage post : getResp.getPosts()) {
        try {
            // Unwrap AES key with ElGamal private key
            byte[] wrappedKey = java.util.Base64.getDecoder().decode(post.getWrappedKey());
            Cipher elgamal = Cipher.getInstance("ElGamal/None/PKCS1Padding", "BC");
            elgamal.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKeyBytes = elgamal.doFinal(wrappedKey);

            // Decrypt the message
            String plaintext = post.getDecryptedPayload(aesKeyBytes);

            System.out.println("From: " + post.getUser());
            System.out.println("Message: " + plaintext);
            System.out.println("--------------");
        } catch (Exception e) {
            System.out.println("[Error decrypting post]: " + e.getMessage());
        }
    }

    channel.closeChannel();
}


    /**
     * Main entry point for the client program.
     * If the --create flag is passed, this will create a new account on the server.
     * If the --create flag is not passed, this will retrieve all posts from the server
     * for the specified user and decrypt them using the user's ElGamal private key.
     * @param args command line arguments
     * @throws Exception if an error occurs while executing the program
     */
    public static void main(String[] args) throws Exception {
        // Register Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
        
        System.setProperty("javax.net.ssl.trustStore", "truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "test12345");
        
        processArgs(args);

        if (create) {
            System.out.print("Enter a password: ");
            String password = new String(System.console().readPassword());
        
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ElGamal");
            keyGen.initialize(512); //FOR TESTING
            //keyGen.initialize(2048);
            KeyPair kp = keyGen.generateKeyPair();
        
            String pubKeyEncoded = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            String privKeyEncoded = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        
        
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            
            socket.startHandshake(); // 👈 force the TLS handshake now


            channel = new ProtocolChannel(socket);
            channel.addMessageType(new StatusMessage());
        
            CreateMessage msg = new CreateMessage(user, password, pubKeyEncoded);
            channel.sendMessage((Message) msg);
    
            
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
                System.out.println("Private Key:\n" + privKeyEncoded);

                @SuppressWarnings("static-access")
                String totpKey = status.getPayload();
                byte[] totpBytes = Base64.getDecoder().decode(totpKey);
                String base32Totp = Base32.encodeToString(totpBytes, true); // no padding
                System.out.println("Base 32 Key:\n" + base32Totp);
            } else {
                System.out.println("Failed to create account: ");
            }
        
            channel.closeChannel();
        }
    }
}
