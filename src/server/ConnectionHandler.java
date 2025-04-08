package server;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.Socket;
import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.PostBuilder;
import common.protocol.messages.PostMessage;
import common.protocol.messages.StatusMessage;
import merrimackutil.util.NonceCache;

public class ConnectionHandler implements Runnable {

    private ProtocolChannel channel;
    private byte[] sessionKey;

    /**
     * Constructs a new connection handler for the given connection.
     * @param sock the socket to communicate over.
     * @param doDebug if tracing should be turned on or not.
     * @param serviceName the name of the service.
     * @param secret the secret.
     * @param nonceCache the nonce cache of the daemon.
     * @throws IllegalArgumentException if the socket is invalid.
     * @throws IOException if we can't read or write from the channel.
     */
    public ConnectionHandler(Socket sock, boolean doDebug, String serviceName, String secret, NonceCache nonceCache) throws IllegalArgumentException, IOException {
        this.channel = new ProtocolChannel(sock);
    }

    /**
     * Handles the Builtin service connection.
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
        Message msg = null;
        String payload = null;

        try {
            // Receive the message from the client
            msg = channel.receiveMessage();
            
            // Check the type of the received message using getType()
            if (msg.getType().equals("Create")) {
                handleCreateMessage((Message) msg);
                System.out.println("Received a CreateMessage.");
            } else if (msg.getType().equals("Post")) {
                // Handle PostMessage (as in original)
                handlePostMessage((PostMessage) msg);
            } else {
                System.out.println("Received an unsupported message type: " + msg.getType());
            }
        } catch (InvalidObjectException ex) {
            System.out.println("Error decoding message: " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("Unexpected error: " + ex.getMessage());
            System.out.println("Recieved:"+msg);
        }
    }

    /**
     * Handle account creation (CreateMessage).
     * @param createMessage the CreateMessage containing user info.
     */
    private void handleCreateMessage(Message createMessage) {
        // Simulate checking if the user exists
        boolean userExists = checkIfUserExists(createMessage.getUsername());
        
        StatusMessage statusMessage;

        if (userExists) {
            // If the user already exists, return failure status with appropriate message.
            statusMessage = new StatusMessage(false, "User already exists.");
        } else {
            // Simulate creating the user and generating a TOTP key
            try {
                // Here you can implement your logic for actually creating the user in your database
                String totpKey = generateTOTPKey();
                
                // Successfully created user, respond with success status and TOTP key.
                statusMessage = new StatusMessage(true, totpKey);
            } catch (Exception e) {
                // If there was an error during creation, respond with failure
                statusMessage = new StatusMessage(false, "Unexpected error.");
            }
        }

        // Send the response message back to the client
        channel.sendMessage((Message) statusMessage);
    }

    /**
     * Simulate checking if the user already exists in the system.
     * @param username the username to check.
     * @return true if the user exists, false otherwise.
     */
    private boolean checkIfUserExists(String username) {
        // In a real system, you'd check a database or other data store here
        return false; // Assume user does not exist for now
    }

    /**
     * Simulate TOTP key generation.
     * @return the generated TOTP key as a Base64-encoded string.
     */
    private String generateTOTPKey() {
        // In a real system, you would generate a TOTP key here (e.g., using a library like Google Authenticator)
        return "TOTP-SECRET-KEY";  // For testing purposes, returning a hardcoded string
    }

    /**
     * Handle PostMessage (unchanged from your original implementation).
     * @param postMessage the PostMessage to handle.
     */
    private void handlePostMessage(PostMessage postMessage) {
        String payload = postMessage.getDecryptedPayload(sessionKey);
        if (payload != null) {
            // Process the post message and send an appropriate response
            channel.sendMessage((Message) PostBuilder.buildMessage(payload));
        }
    }
}
