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

    public void sendMessage(String user, String recvr, String plaintext) throws Exception {
        System.out.println("[Debug Post Client] Starting sendMessage to " + recvr + " with plaintext: " + plaintext);

        // Step 1: Request recipient's public key
        System.out.println("[Debug Post Client] Sending public key request...");
        PubKeyRequest pubKeyRequest = new PubKeyRequest(recvr);
        channel.sendMessage(pubKeyRequest);

        // Step 2: Receive public key response
        StatusMessage pubKeyResponse = (StatusMessage) channel.receiveMessage();
        System.out.println("[Debug Post Client] Received public key response: " + StatusMessage.getPayload());
        if (!pubKeyResponse.getStatus()) {
            System.out.println("[Debug Post Client] Failed to retrieve public key: " + StatusMessage.getPayload());
            return;
        }

        // Step 3: Decode recipient's ElGamal public key
        byte[] pubKeyBytes = Base64.getDecoder().decode(StatusMessage.getPayload());
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("ElGamal", "BC");
        PublicKey recipientPubKey = keyFactory.generatePublic(keySpec);
        System.out.println("[Debug Post Client] Decoded recipient public key.");

        // Step 4: Generate a 256-bit AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();
        System.out.println("[Debug Post Client] Generated AES key.");

        // Step 5: Encrypt the message using AES/GCM/NoPadding
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        System.out.println("[Debug Post Client] Generated IV: " + Base64.getEncoder().encodeToString(iv));

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] ciphertext = aesCipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        System.out.println("[Debug Post Client] AES-encrypted ciphertext: " + Base64.getEncoder().encodeToString(ciphertext));

        // Step 6: Encrypt the AES key using ElGamal (key wrapping)
        Cipher elgamalCipher = Cipher.getInstance("ElGamal/None/PKCS1Padding", "BC");
        elgamalCipher.init(Cipher.ENCRYPT_MODE, recipientPubKey);
        byte[] wrappedKey = elgamalCipher.doFinal(aesKey.getEncoded());
        System.out.println("[Debug Post Client] AES key encrypted with ElGamal.");

        // Step 7: Construct and send the PostMessage
        PostMessage post = new PostMessage(
            user,
            Base64.getEncoder().encodeToString(ciphertext),
            Base64.getEncoder().encodeToString(wrappedKey),
            Base64.getEncoder().encodeToString(iv)
        );
        System.out.println("[Debug Post Client] Sending PostMessage..."+post);

        channel.sendMessage(post);

        // Step 8: Receive and display status response
        StatusMessage response = (StatusMessage) channel.receiveMessage();
        System.out.println("[Debug Post Client] Received status response.");
        if (response.getStatus()) {
            System.out.println("[Debug Post Client] Message sent successfully: " + response.getPayload());
        } else {
            System.out.println("[Debug Post Client] Failed to post message: " + response.getPayload());
        }

        // Step 9: Close the channel
        System.out.println("[Debug Post Client] Closing channel.");
        channel.closeChannel();
    }
}
