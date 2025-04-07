package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.bouncycastle.util.Objects;

import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.PostMessage;
import common.protocol.messages.PubKeyRequest;
import common.protocol.messages.StatusMessage;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
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
            PostClient client = new PostClient(socket);
            client.sendMessage(recvr, message);
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
        processArgs(args);
    }

    public class PostClient {
        private final ProtocolChannel channel;
    
        public PostClient(Socket socket) throws Exception {
            this.channel = new ProtocolChannel(socket);
        }
    
        public void sendMessage(String recipient, String messageText) throws Exception {
            // 1. Initialize StatusMessage for recipient's public key request
            StatusMessage statusMessage = new StatusMessage("Requesting public key for " + recipient);
            channel.sendMessage((Message) statusMessage);  // Send the StatusMessage object
    
            // 2. Request recipient's public key
            PubKeyRequest req = new PubKeyRequest(recipient);
            channel.sendMessage((Message) req);
    
            StatusMessage pubKeyResp = channel.sendMessage(StatusMessage.class);
            if (pubKeyResp == null || !pubKeyResp.payload.equals("Success")) {
                System.out.println("Recipient not found.");
                return;
            }
    
            // 3. Decode public key
            byte[] pubKeyBytes = Base64.getDecoder().decode(pubKeyResp.payload);
            PublicKey pubKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(pubKeyBytes));
    
            // 4. Encrypt the message using AES-GCM
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey aesKey = keyGen.generateKey();
    
            byte[] iv = new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
    
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
            byte[] ciphertext = aesCipher.doFinal(messageText.getBytes(StandardCharsets.UTF_8));
    
            // 5. Encrypt AES key using recipient's RSA public key
            Cipher rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] wrappedKey = rsaCipher.doFinal(aesKey.getEncoded());
    
            // 6. Send PostMessage
            PostMessage post = new PostMessage(
                    recipient,
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(wrappedKey),
                    Base64.getEncoder().encodeToString(iv), messageText, messageText, messageText
            );
    
            channel.sendMessage((Message) post);
    
            // 7. Final response
            StatusMessage postResp = channel.sendMessage(StatusMessage.class);
            if (postResp != null) {
                System.out.println("Post Response: " + postResp.payload);
            } else {
                System.err.println("Failed to post the message.");
            }
        }
    }
    

    
}
