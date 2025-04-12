package client;

import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.AuthenticateMessage;
import common.protocol.user_creation.CreateMessage;
import common.protocol.messages.GetMessage;
import common.protocol.messages.GetResponseMessage;
import common.protocol.messages.PostMessage;
import common.protocol.messages.PubKeyRequest;
import common.protocol.messages.StatusMessage;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.codec.Base32;
import merrimackutil.util.NonceCache;
import merrimackutil.util.Tuple;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class Client {
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
        System.out.println("Usage:");
        System.out.println("  client --create --user <user> --host <host> --port <portnum>");
        System.out.println("  client --post <msg> --user <user> --recvr <user> --host <host> --port <portnum>");
        System.out.println("  client --get --key <privkey> --user <user> --host <host> --port <portnum>");
        System.exit(1);
    }

    public static void processArgs(String[] args) throws Exception {
        LongOption[] opts = new LongOption[] {
            new LongOption("create", false, 'c'),
            new LongOption("post", true, 'o'),
            new LongOption("get", false, 'g'),
            new LongOption("recvr", true, 'r'),
            new LongOption("key", true, 'k'),
            new LongOption("user", true, 'u'),
            new LongOption("host", true, 'h'),
            new LongOption("port", true, 'p')
        };

        OptionParser parser = new OptionParser(args);
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
                case 'p': port = Integer.parseInt(currOpt.getSecond()); break;
                default: usage(); break;
            }
        }

        if (user == null || host == null || port == 0) usage();

        if (create) {
            handleCreate();
        } else if (post) {
            if (recvr == null || message == null) usage();
            handlePost();
        } else if (get) {
            if (privKey == null) usage();
            handleGet();
        } else {
            usage();
        }
    }

    private static void handleCreate() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter a password: ");
        String password = reader.readLine();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ElGamal");
        keyGen.initialize(512); // testing size
        KeyPair kp = keyGen.generateKeyPair();

        String pubKeyEncoded = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        String privKeyEncoded = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

        System.out.println("Public key: " + pubKeyEncoded);
        System.out.println("Private key: " + privKeyEncoded);

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
        socket.startHandshake();

        ProtocolChannel channel = new ProtocolChannel(socket);
        channel.addMessageType(new StatusMessage());

        CreateMessage msg = new CreateMessage(user, password, pubKeyEncoded);
        channel.sendMessage(msg);

        Message response = channel.receiveMessage();
        if (!(response instanceof StatusMessage)) {
            System.out.println("Unexpected response");
            return;
        }

        StatusMessage status = (StatusMessage) response;
        if (status.getStatus()) {
            System.out.println("Account created successfully.");
            System.out.println("Private Key:\n" + privKeyEncoded);

            byte[] totpBytes = Base64.getDecoder().decode(status.getPayload());
            String base32Totp = Base32.encodeToString(totpBytes, true);
            System.out.println("Base 32 Key:\n" + base32Totp);
        } else {
            System.out.println("Error: " + status.getPayload());
        }

        channel.closeChannel();
    }

    private static void handlePost() throws Exception {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
        socket.startHandshake();

        ProtocolChannel channel = new ProtocolChannel(socket);
        channel.addMessageType(new AuthenticateMessage());
        channel.addMessageType(new StatusMessage());
        channel.addMessageType(new PostMessage());
        channel.addMessageType(new PubKeyRequest());

        if (!authenticateUser(channel)) {
            System.out.println("Authentication failed.");
            return;
        }

        System.out.println("Authenticated.");
        PostClient postClient = new PostClient(socket); // reuse the same socket
        postClient.sendMessage(recvr, message);
    }

    private static void handleGet() throws Exception {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
        socket.startHandshake();

        ProtocolChannel channel = new ProtocolChannel(socket);
        channel.addMessageType(new AuthenticateMessage());
        channel.addMessageType(new StatusMessage());
        channel.addMessageType(new GetMessage());
        channel.addMessageType(new GetResponseMessage());
        channel.addMessageType(new PostMessage());

        if (!authenticateUser(channel)) {
            System.out.println("Authentication failed.");
            return;
        }

        byte[] privKeyBytes = Base64.getDecoder().decode(privKey);
        KeyFactory keyFactory = KeyFactory.getInstance("ElGamal", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(new X509EncodedKeySpec(privKeyBytes));

        channel.sendMessage(new GetMessage(user));
        Message response = channel.receiveMessage();

        if (response instanceof StatusMessage) {
            System.out.println("Error: " + ((StatusMessage) response).getPayload());
        } else if (response instanceof GetResponseMessage) {
            GetResponseMessage getResp = (GetResponseMessage) response;
            for (PostMessage post : getResp.getPosts()) {
                try {
                    byte[] wrappedKey = Base64.getDecoder().decode(post.getWrappedKey());
                    Cipher elgamal = Cipher.getInstance("ElGamal/None/PKCS1Padding", "BC");
                    elgamal.init(Cipher.DECRYPT_MODE, privateKey);
                    byte[] aesKeyBytes = elgamal.doFinal(wrappedKey);
                    String plaintext = post.getDecryptedPayload(aesKeyBytes);

                    System.out.println("From: " + post.getUser());
                    System.out.println("Message: " + plaintext);
                    System.out.println("--------------");
                } catch (Exception e) {
                    System.out.println("[Error decrypting post]: " + e.getMessage());
                }
            }
        }

        channel.closeChannel();
    }

    private static boolean authenticateUser(ProtocolChannel channel) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter password: ");
        String password = reader.readLine();
        System.out.print("Enter OTP: ");
        String otp = reader.readLine();

        AuthenticateMessage authMsg = new AuthenticateMessage(user, password, otp);
        channel.sendMessage(authMsg);
        Message response = channel.receiveMessage();

        return response instanceof StatusMessage && ((StatusMessage) response).getStatus();
    }

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        System.setProperty("javax.net.ssl.trustStore", "truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "test12345");

        processArgs(args);
    }
}