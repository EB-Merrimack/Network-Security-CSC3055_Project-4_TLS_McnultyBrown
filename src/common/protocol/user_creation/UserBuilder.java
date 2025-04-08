package common.protocol.user_creation;

import common.protocol.Message;
import common.protocol.ProtocolChannel;

public class UserBuilder {

    private String user;
    private String password;
    private String publicKey;

    // Setters for the fields
    public UserBuilder setUser(String user) {
        this.user = user;
        return this; // Return the current builder to allow method chaining
    }

    public UserBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public UserBuilder setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    // Build method to create a UserMessage instance
    public UserMessage build() {
        // Validate the required fields before creating the UserMessage
        if (user == null || password == null || publicKey == null) {
            throw new IllegalStateException("User, password, and public key must be set before building.");
        }
        return new UserMessage(user, password, publicKey);
    }

    // Implementing buildMessage to extract data from a string and create a message
    public static Message buildMessage(String input) {
        // Assuming the input string follows a specific format: 
        // "user:<user>, password:<password>, publicKey:<publicKey>"
        // Remove curly braces from the input string
        input = input.replaceAll("[{}]", "");
        // Parse the input string
        String[] parts = input.split(", ");
        String user = null;
        String password = null;
        String publicKey = null;
        
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                // Extract the values for each field
                switch (key) {
                    case "username":
                        user = value;
                        break;
                    case "password":
                        password = value;
                        break;
                    case "publickey":
                        publicKey = value;
                        break;
                    case "type":
                        // Skip the type field
                        break;
                    default:
                        System.err.println("Unknown key: " + key);
                        break;
                }
            }
        }

        // Validate that all necessary fields are present
        if (user == null || password == null || publicKey == null) {
            throw new IllegalArgumentException("Missing required fields in the input string.");
        }

        // Use UserBuilder to create the UserMessage
        UserBuilder builder = new UserBuilder()
            .setUser(user)
            .setPassword(password)
            .setPublicKey(publicKey);

        return (Message) builder.build(); // Return the created UserMessage
    }
}
