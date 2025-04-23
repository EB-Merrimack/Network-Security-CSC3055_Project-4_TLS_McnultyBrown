package client;

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

/**
 * Sends an encrypted message to a specified recipient.
 *
 * This method performs the following steps:
 * 1. Requests the recipient's public key.
 * 2. Receives and verifies the public key response.
 * 3. Decodes the recipient's ElGamal public key.
 * 4. Generates a 256-bit AES key for encryption.
 * 5. Encrypts the message using AES/GCM/NoPadding.
 * 6. Encrypts the AES key using the ElGamal public key (key wrapping).
 * 7. Constructs and sends a PostMessage containing the encrypted message, 
 *    wrapped key, and initialization vector.
 * 8. Attempts to receive a status response, retrying if necessary.
 * 9. Closes the channel after message transmission.
 *
 * @param user the username of the sender
 * @param recvr the username of the recipient
 * @param plaintext the message to be sent in plaintext
 * @throws Exception if any error occurs during the encryption or message sending process
 */

    public void sendMessage(String user, String recvr, String plaintext) throws Exception {

        // Step 1: Request recipient's public key
        PubKeyRequest pubKeyRequest = new PubKeyRequest(recvr);
        channel.sendMessage(pubKeyRequest);

        // Step 2: Receive public key response
        StatusMessage pubKeyResponse = (StatusMessage) channel.receiveMessage();
        if (!pubKeyResponse.getStatus()) {
            return;
        }

        // Step 3: Decode recipient's ElGamal public key
        byte[] pubKeyBytes = Base64.getDecoder().decode(StatusMessage.getPayload());
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("ElGamal", "BC");
        PublicKey recipientPubKey = keyFactory.generatePublic(keySpec);

        // Step 4: Generate a 256-bit AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();

        // Step 5: Encrypt the message using AES/GCM/NoPadding
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] ciphertext = aesCipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        

        // Step 6: Encrypt the AES key using ElGamal (key wrapping)
        Cipher elgamalCipher = Cipher.getInstance("ElGamal/None/PKCS1Padding", "BC");
        elgamalCipher.init(Cipher.ENCRYPT_MODE, recipientPubKey);
        byte[] wrappedKey = elgamalCipher.doFinal(aesKey.getEncoded());


        // Step 7: Construct and send the PostMessage
        PostMessage post = new PostMessage(
            recvr,
            Base64.getEncoder().encodeToString(ciphertext),
            Base64.getEncoder().encodeToString(wrappedKey),
            Base64.getEncoder().encodeToString(iv)
        );
    

        channel.sendMessage(post);
   
        
        StatusMessage response = null;
        int maxRetries = 10;
        int retryDelayMillis = 500;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Object msg = channel.receiveMessage();
                if (msg instanceof StatusMessage) {
                    response = (StatusMessage) msg;
                    break;  // Exit loop once we get the valid response
                } else {
                    
                }
            } catch (NullPointerException e) {
            
            } catch (Exception e) {
                e.printStackTrace();
            }
        
            try {
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;  // Exit loop if interrupted
            }
        }
        
        if (response != null) {
            // Check the status and print the result accordingly
            if (response.getStatus()) {
                System.out.println("Message sent successfully: ") ;
            } else {
                System.out.println(" Failed to post message: " );
            }
        } else {
            System.out.println(" No valid status response received.");
        }
        
        // Step 9: Close the channel
        channel.closeChannel();
        
}
}