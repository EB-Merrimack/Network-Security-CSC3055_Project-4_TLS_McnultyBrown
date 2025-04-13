
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
        this.channel.addMessageType(new PubKeyRequest());
        this.channel.addMessageType(new PostMessage());
        this.channel.addMessageType(new GetMessage());
        this.channel.addMessageType(new GetResponseMessage());
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
                Message msg = null;
    
                try {
                    // Try to receive the message
                    msg = channel.receiveMessage();
                } catch (NullPointerException e) {
                    // If a NullPointerException occurs, log it and continue waiting for the next message
                    System.err.println("[ERROR] NullPointerException encountered while receiving message.");
                    System.err.println("[DEBUG] Received message: " + msg);
                    // You can decide whether to break out of the loop or continue waiting
                    continue; // Continue waiting for the next message
                }
                System.out.println("[DEBUG] Received message: " + msg);
            if (msg.getType().equals("Create")) {
                // Handle CreateMessage 
                handleCreateMessage(msg);
                return;
            } else if (msg.getType().equals("authenticate")) {
            boolean success = AuthenticationHandler.authenticate((AuthenticateMessage) msg);

            if (success) {
                channel.sendMessage(new StatusMessage(true, "Authentication successful."));
            } else {
                channel.sendMessage(new StatusMessage(false, "Authentication failed. Check your password or OTP."));
            }
            return;
        } else if (msg.getType().equals("PubKeyRequest")) {
            System.out.println("[SERVER] Received PubKeyRequest.");
        
            PubKeyRequest pubKeyRequest = (PubKeyRequest) msg;
            String username = pubKeyRequest.getUser();  // Use getUser() here
            System.out.println("[SERVER] Public key requested for user: " + username);
        
            String base64Key = UserDatabase.getEncodedPublicKey(username) ;  // You might want to change this to take a username
            System.out.println("[SERVER] Sending public key (Base64): " + base64Key);
        
            channel.sendMessage((Message) new StatusMessage(true, base64Key));
            System.out.println("[SERVER] Public key sent.");

        } else if (msg.getType().equals("post")) {
            // Handle PostMessage
            System.out.println("[SERVER] Handling PostMessage");
           handlePostMessage((PostMessage) msg);
           return;
        }
          else if (msg instanceof GetMessage) {
                GetMessage getMsg = (GetMessage) msg;
                String username = getMsg.getUser();
            
                board.loadFromFile(); // ensure latest board
            
                // ✅ Step 1: Find all posts addressed to the requested user
                List<Post> userPosts = new ArrayList<>();
                for (Post post : board.getPosts()) {
                    if (post.getUser().equals(username)) {
                        userPosts.add(post);
                    }
                }
            
                // ✅ Step 2: Convert Post → PostMessage
                List<PostMessage> converted = new ArrayList<>();
                for (Post post : userPosts) {
                    converted.add(post.toPostMessage()); // make sure to add this helper in Post.java
                }
            
                // ✅ Step 3: Send response
                GetResponseMessage response = new GetResponseMessage(converted);
                channel.sendMessage(response);
            
        } else {
            System.out.println("[SERVER] Unknown or unsupported message type: " + msg.getType());
        }

    }
}catch (Exception ex) {
        ex.printStackTrace();
    }
}

            
       

        /**
         * Handles a CreateMessage sent by the client. Creates a new user account using the
         * username, password, and public key provided in the message, and saves the
         * account information to the user database file specified in the
         * Configuration. Sends a StatusMessage back to the client with a boolean indicating
         * success or failure and a message containing the base64 encoded TOTP key if
         * successful, or an error message otherwise.
         * 
         * @param msg the CreateMessage received from the client
         */
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
        /**
         * Handles a PostMessage and adds the post to the board.
         * @param postMsg the PostMessage to be handled
         */
    private void handlePostMessage(PostMessage postMsg) {
        try {
            System.out.println("[SERVER] Handling PostMessage");
    
            // reformat PostMessage to Post
            String Type=postMsg.getType();
            String User=postMsg.getUser();
            String Message=postMsg.getMessage();
            String WrappedKey=postMsg.getWrappedKey();
            String IV=postMsg.getIv();
    
            Post post = new Post( User, Message, WrappedKey, IV,Type);
            // Add post to board and save
            board.addPost(post);
            board.loadAndAddPost(post);
    
            channel.sendMessage(new StatusMessage(true, "Success!"));
            System.out.println("[SERVER] Post successful.");
            return;
    
        } catch (Exception e) {
            e.printStackTrace();
        }
                channel.sendMessage(new StatusMessage(false, "Post failed due to server error."));
    }
    
    
}
