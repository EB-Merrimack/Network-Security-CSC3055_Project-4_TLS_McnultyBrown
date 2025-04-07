package client;

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

import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.PostMessage;
import common.protocol.messages.PubKeyRequest;
import common.protocol.messages.StatusMessage;

public class PostClient {
  private final ProtocolChannel channel;
    
        public PostClient(Socket socket) throws Exception {
            this.channel = new ProtocolChannel(socket);
        }
    
        public void sendMessage(String recipient, String messageText) throws Exception {
            // 1. Initialize StatusMessage for recipient's public key request
            StatusMessage statusMessage = new StatusMessage(true, "Requesting public key for " + recipient);
            channel.sendMessage((Message) statusMessage);  // Send the StatusMessage object
    
            // 2. Request recipient's public key
            PubKeyRequest req = new PubKeyRequest(recipient);
            channel.sendMessage((Message) req);
    
            StatusMessage pubKeyResp = channel.sendMessage(StatusMessage.class);
            if (pubKeyResp == null || !pubKeyResp.getPayload().equals("Success")) {
                System.out.println("Recipient not found.");
                return;
            }
    
            // 3. Decode public key
            byte[] pubKeyBytes = Base64.getDecoder().decode(pubKeyResp.getPayload());
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
                System.out.println("Post Response: " + postResp.getPayload());
            } else {
                System.err.println("Failed to post the message.");
            }
        }
    }
    
