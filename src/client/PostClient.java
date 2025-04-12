package client;

import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.PostMessage;
import common.protocol.messages.PubKeyRequest;
import common.protocol.messages.StatusMessage;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.net.Socket;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class PostClient {
    private final ProtocolChannel channel;

    public PostClient(Socket socket) throws IOException {
        this.channel = new ProtocolChannel(socket);
        channel.addMessageType(new PubKeyRequest());
        channel.addMessageType(new PostMessage());
        channel.addMessageType(new StatusMessage());
    }

    public void sendMessage(String receiver, String plaintext) throws Exception {
        System.out.println("[CLIENT] Starting sendMessage...");
        
        // Step 1: Send public key request
        System.out.println("[CLIENT] Sending PubKeyRequest for user: " + receiver);
        PubKeyRequest request = new PubKeyRequest(receiver);
        channel.sendMessage(request);
    
        System.out.println("[CLIENT] Waiting for PubKeyRequest response...");
        Message response = channel.receiveMessage();
    
        if (response == null) {
            System.out.println("[CLIENT] ERROR: No response received from server (null).");
            return;
        }
    
        System.out.println("[CLIENT] Got response class: " + response.getClass().getName());
    
        if (!(response instanceof StatusMessage)) {
            throw new RuntimeException("Unexpected response to PubKeyRequest");
        }
    
        StatusMessage status = (StatusMessage) response;
        if (!status.getStatus()) {
            throw new RuntimeException("No such user: " + status.getPayload());
        }
    
        // Step 2: Parse recipient’s public key
        String pubKeyBase64 = status.getPayload();
        byte[] pubKeyBytes = Base64.getDecoder().decode(pubKeyBase64);
        KeyFactory factory = KeyFactory.getInstance("ElGamal", "BC");
        PublicKey pubKey = factory.generatePublic(new X509EncodedKeySpec(pubKeyBytes));
    
        System.out.println("[CLIENT] Parsed public key, proceeding to encrypt message...");
    
        // Step 3: Generate AES key + encrypt message
        SecretKey aesKey = KeyGenerator.getInstance("AES").generateKey();
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        aes.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        byte[] cipherText = aes.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    
        // Step 4: Wrap AES key with recipient's public key
        Cipher rsa = Cipher.getInstance("ElGamal/None/PKCS1Padding", "BC");
        rsa.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] wrappedKey = rsa.doFinal(aesKey.getEncoded());
    
        // Step 5: Build and send PostMessage
        PostMessage msg = new PostMessage(receiver,
            Base64.getEncoder().encodeToString(cipherText),
            Base64.getEncoder().encodeToString(wrappedKey),
            Base64.getEncoder().encodeToString(iv));
    
        System.out.println("[CLIENT] Sending encrypted PostMessage...");
        channel.sendMessage(msg);
    
        System.out.println("[CLIENT] Waiting for post confirmation...");
        Message confirm = channel.receiveMessage();
    
        if (confirm == null) {
            System.out.println("[CLIENT] ERROR: No confirmation received (null).");
            return;
        }
    
        if (confirm instanceof StatusMessage) {
            StatusMessage m = (StatusMessage) confirm;
            System.out.println("[CLIENT] Post status: " + m.getPayload());
        } else {
            System.out.println("[CLIENT] Unexpected message type: " + confirm.getClass().getSimpleName());
        }
    }
}
