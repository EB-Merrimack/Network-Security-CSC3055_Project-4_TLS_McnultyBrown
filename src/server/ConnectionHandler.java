
package server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.AuthenticateMessage;
import common.protocol.messages.GetMessage;
import common.protocol.messages.GetResponseMessage;
import common.protocol.messages.PostMessage;
import common.protocol.messages.PubKeyRequest;
import common.protocol.messages.StatusMessage;
import common.protocol.user_auth.AuthenticationHandler;
import common.protocol.user_auth.UserDatabase;
import merrimackutil.util.NonceCache;
import common.Board;
import common.protocol.post.Post;

public class ConnectionHandler implements Runnable {

    private ProtocolChannel channel;
    private NonceCache nonceCache;
    private boolean doDebug = false;
    private String serviceName;
    private String secret;
    private byte[] sessionKey;
    private static Board board = new Board();

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
        this.channel.addMessageType(new PostMessage());
        this.channel.addMessageType(new AuthenticateMessage());
        this.channel.addMessageType(new GetMessage());
        this.channel.addMessageType(new GetResponseMessage());
        this.channel.addMessageType(new PubKeyRequest());

        this.doDebug = doDebug;

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
            board.loadFromFile();
    
            while (true) {
                System.out.println("[DEBUG] Waiting to receive a message...");
                Message msg = channel.receiveMessage();
                System.out.println("[DEBUG] Received message of type: " + msg.getType());
    
                if (msg.getType().equals("Create")) {
                    handleCreateMessage(msg);
                } else if (msg instanceof AuthenticateMessage) {
                    boolean success = AuthenticationHandler.authenticate((AuthenticateMessage) msg);
                    if (success) {
                        channel.sendMessage(new StatusMessage(true, ""));
                    } else {
                        channel.sendMessage(new StatusMessage(false, "Authentication failed."));
                        return; // 💥 Exit on failed auth only
                    }
                } else if (msg instanceof PubKeyRequest) {
                    PubKeyRequest req = (PubKeyRequest) msg;
                    String targetUser = req.getUser();
    
                    System.out.println("[SERVER] PubKeyRequest for user: " + targetUser);
    
                    if (!UserDatabase.check(targetUser)) {
                        System.out.println("[SERVER] PubKeyRequest failed: user not found.");
                        channel.sendMessage(new StatusMessage(false, "Request Failed"));
                    } else {
                        String pubKey = UserDatabase.get(targetUser).getPubkey();
                        System.out.println("[SERVER] Found pubkey for " + targetUser);
                        channel.sendMessage(new StatusMessage(true, pubKey));
                    }
                } else if (msg instanceof PostMessage) {
                    PostMessage postMsg = (PostMessage) msg;
                    Post post = new Post(postMsg.getUser(), postMsg.getWrappedKey(), postMsg.getIv(), postMsg.getMessage());
    
                    board.addPost(post);
                    board.saveToFile();
    
                    channel.sendMessage(new StatusMessage(true, "Message posted."));
                } else {
                    System.out.println("[SERVER] Unknown or unsupported message type: " + msg.getType());
                    channel.sendMessage(new StatusMessage(false, "Unknown Message."));
                }
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
                common.protocol.user_creation.CreateAccount.createAccount(username, password, publicKey, userfile);
    
            // Send the response back to the client
            channel.sendMessage(response);
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
