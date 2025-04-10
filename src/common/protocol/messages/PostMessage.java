package common.protocol.messages;

import common.protocol.Message;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import java.io.InvalidObjectException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Represents a message posted to the bulletin board.
 */
public class PostMessage implements Message {
    private String user;        // recipient username
    private String message;     // base64 AES ciphertext
    private String wrappedkey;  // base64 ElGamal-wrapped AES key
    private String iv;          // base64 AES IV

    public PostMessage() {}

    public PostMessage(String user, String message, String wrappedkey, String iv) {
        this.user = user;
        this.message = message;
        this.wrappedkey = wrappedkey;
        this.iv = iv;
    }

    public String getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public String getWrappedKey() {
        return wrappedkey;
    }

    public String getIv() {
        return iv;
    }
    
    public String getDecryptedPayload(byte[] sessionKey) {
    try {
        byte[] ivBytes = Base64.getDecoder().decode(getIv());
        byte[] cipherBytes = Base64.getDecoder().decode(getMessage());

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, ivBytes);
        SecretKeySpec keySpec = new SecretKeySpec(sessionKey, "AES");

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        byte[] plainBytes = cipher.doFinal(cipherBytes);

        return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
    } catch (Exception e) {
        throw new RuntimeException("Failed to decrypt payload", e);
    }
}

    @Override
    public String getType() {
        return "post";
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", "post");
        obj.put("user", user);
        obj.put("message", message);
        obj.put("wrappedkey", wrappedkey);
        obj.put("iv", iv);
        return obj;
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!(obj instanceof JSONObject)) {
            throw new InvalidObjectException("Expected JSONObject.");
        }

        JSONObject json = (JSONObject) obj;
        this.user = json.getString("user");
        this.message = json.getString("message");
        this.wrappedkey = json.getString("wrappedkey");
        this.iv = json.getString("iv");
    }

    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        return new PostMessage(
            obj.getString("user"),
            obj.getString("message"),
            obj.getString("wrappedkey"),
            obj.getString("iv")
        );
    }

    @Override
    public String toString() {
        return "[PostMessage] to=" + user;
    }
}