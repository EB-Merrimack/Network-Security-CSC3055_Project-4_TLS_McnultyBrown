package common.protocol.messages;

/**
 * Builder for PostMessage objects.
 */
public class PostBuilder {
    // Traditional builder pattern (optional)
    private String sender;
    private String message;
    private String wrappedKey;
    private String iv;
    private String recipient;
    private String ciphertext;
    private String encryptedKey;

    public PostBuilder() {}

    public PostBuilder setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public PostBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public PostBuilder setWrappedKey(String wrappedKey) {
        this.wrappedKey = wrappedKey;
        return this;
    }

    public PostBuilder setIv(String iv) {
        this.iv = iv;
        return this;
    }

    public PostBuilder setRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public PostBuilder setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
        return this;
    }

    public PostBuilder setEncryptedKey(String encryptedKey) {
        this.encryptedKey = encryptedKey;
        return this;
    }

    public PostMessage build() {
        return new PostMessage(
            sender,
            message,
            wrappedKey,
            iv,
            recipient,
            ciphertext,
            encryptedKey
        );
    }

    /**
     * Static helper to build a PostMessage from a string payload.
     * Assumes defaults for testing/demo. Fill in or modify as needed.
     */
    public static PostMessage buildMessage(String payload) {
        return new PostMessage(
            "server",              // sender (or user)
            payload,               // plain message
            "dummyWrappedKey",     // placeholder key
            "dummyIv",             // placeholder IV
            "recipient",           // recipient
            "ciphertext",          // optionally set if needed
            "dummyEncryptedKey"    // placeholder encrypted key
        );
    }
}
