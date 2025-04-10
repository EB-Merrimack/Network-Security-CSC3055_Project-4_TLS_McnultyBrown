
package server;

import java.io.IOException;
import java.net.Socket;
import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.AuthenticateMessage;
import common.protocol.messages.PostMessage;
import common.protocol.messages.StatusMessage;
import common.protocol.user_auth.AuthenticationHandler;
import merrimackutil.util.NonceCache;

public class ConnectionHandler implements Runnable {

    private ProtocolChannel channel;
    private NonceCache nonceCache;
    private boolean doDebug = false;
    private String serviceName;
    private String secret;
    private byte[] sessionKey;
    /**
     * Constructs a new connection handler for the given connection.
     * @param sock the socket to communicate over.
     * @param doDebug if tracing should be turned on or not.
     * @param serviceName the name of the service.
     * @param secret the secret.
     * @param nonceCache the nonce cache of the daemon.
     * @throws IllegalArgumentException the socket is invalid.
     * @throws IOException we can't read or write from the channel.
     */
    public ConnectionHandler(Socket sock, boolean doDebug, String serviceName, String secret, NonceCache nonceCache) throws IllegalArgumentException, IOException
    {
        this.channel = new ProtocolChannel(sock);
        this.channel.addMessageType(new common.protocol.user_creation.CreateMessage());
        this.channel.addMessageType(new common.protocol.messages.StatusMessage());

        this.nonceCache = nonceCache;
        this.serviceName = serviceName;
        this.secret = secret;
    }

    /**
     * Handles the Bulitin service connection.
     */
    @Override
    public void run() {

        runCommunication();
        channel.closeChannel();
      }

      /**
       * Run the communication between the service and the client after the handshake.
       */
      private void runCommunication() {
        try {
            System.out.println("[DEBUG] Waiting to receive a message...");
            Message msg = channel.receiveMessage();
            System.out.println("[DEBUG] Received message: " + msg);

    
            if (msg.getType().equals("Create")) {
                // Handle CreateMessage (unencrypted)
                handleCreateMessage(msg);
                return;
            } else if (msg.getType().equals("Authenticate")) {
    boolean success = AuthenticationHandler.authenticate((AuthenticateMessage) msg);

    if (success) {
        channel.sendMessage(new StatusMessage(true, "Authentication successful."));
    } else {
        channel.sendMessage(new StatusMessage(false, "Authentication failed. Check your password or OTP."));
    }
    return;
}

            
            else if (msg instanceof PostMessage) {
                // Handle PostMessage (encrypted)
                PostMessage postMsg = (PostMessage) msg;
                String payload = postMsg.getDecryptedPayload(sessionKey);
    
                if (payload == null) {
                    System.out.println("[SERVER] Decrypted payload is null.");
                    return;
                }
    
                System.out.println("[SERVER] Received post payload: " + payload);
                PostMessage responsePost = new PostMessage(
                    postMsg.getUser(),           // recipient
                    payload,                     // decrypted message (plaintext)
                    postMsg.getWrappedKey(),     // re-use original wrapped key
                    postMsg.getIv()              // re-use original IV
                );
                channel.sendMessage(responsePost);
            } else {
                System.out.println("[SERVER] Unknown or unsupported message type: " + msg.getType());
            }
    
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleCreateMessage(Message msg) {
        try {
            System.out.println("[SERVER] Handling CreateMessage");
    
            // Safe cast
            common.protocol.user_creation.CreateMessage createMsg = 
                (common.protocol.user_creation.CreateMessage) msg;
    
            String username = createMsg.getUsername();
            String password = createMsg.getPassword();
            String publicKey = createMsg.getPublicKey();
            String userfile = Configuration.getUsersFile();
    
            System.out.println("[SERVER] Creating account for: " + username);
    
            // Call account creation logic
            common.protocol.messages.StatusMessage response =
                common.protocol.user_creation.AccountCreation.createAccount(username, password, publicKey, userfile);
    
            // Send the response back to the client
            channel.sendMessage(response);
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
