package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.bouncycastle.util.Objects;

import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.CreateMessage;
import common.protocol.messages.PostMessage;
import common.protocol.messages.PubKeyRequest;
import common.protocol.messages.StatusMessage;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.codec.Base32;
import merrimackutil.json.JsonIO;
import merrimackutil.json.parser.JSONParser;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
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
        processArgs(args);
        // 1. Prompt for password
        System.out.print("Enter a password: ");
        String password = new String(System.console().readPassword()); // Hides input

        // 2. Generate ElGamal keypair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ElGamal", "BC");
        keyGen.initialize(2048);
        KeyPair kp = keyGen.generateKeyPair();

        String pubKeyEncoded = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        String privKeyEncoded = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

        // 3. Open TLS connection
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 4. Send Create message
            CreateMessage msg = new CreateMessage(user, password, pubKeyEncoded);
            JsonIO.writeSerializedObject(msg, out);

            // 5. Wait for status response
            String response = in.readLine();
            JSONParser parser = new JSONParser(response);
            // Grab the evaluated JSONType and cast it
            JSONType result = (JSONType) parser.parse().evaluate();

            if (!(result instanceof JSONObject)) {
                throw new InvalidObjectException("Expected JSONObject but got: " + result.getClass().getName());
            }

            JSONObject json = (JSONObject) result;

            // Deserialize into your StatusMessage object
            StatusMessage status = new StatusMessage();
            status.deserialize(json);

            if (status.getStatus()) {
                System.out.println("Account created successfully.");
                System.out.println("Your private key (SAVE THIS SAFELY!):\n" + privKeyEncoded);

                // Convert TOTP key from payload to base32
                String totpKey = Base32.encodeToString(Base64.getDecoder().decode(status.getPayload().getBytes()), false);
                System.out.println("TOTP Secret (Base32 for FreeOTP/Google Auth):\n" + totpKey);
            } else {
                System.out.println("Failed to create account: " + status.getPayload());
            }
        }
    }       
}
