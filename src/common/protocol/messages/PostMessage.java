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
    private String user;
    private String message;
    private String wrappedkey;
    private String iv;
    private String type;

    public PostMessage() {
        this.type = "post";
        System.out.println("[DEBUG] PostMessage default constructor called.");
    }

    public PostMessage(String user, String message, String wrappedkey, String iv) {
        this.type = "post";
        this.user = user;
        this.message = message;
        this.wrappedkey = wrappedkey;
        this.iv = iv;
        System.out.println("[DEBUG] PostMessage constructed: user=" + user + ", iv=" + iv);
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

    public String getType() {
        return type;
    }

    /**
     * Decrypt the AES-encrypted payload using the session key.
     */
    public String getDecryptedPayload(byte[] sessionKey) {
        try {
            byte[] ivBytes = Base64.getDecoder().decode(getIv());
            byte[] cipherBytes = Base64.getDecoder().decode(getMessage());

            System.out.println("[DEBUG] Decrypting payload...");
            System.out.println("[DEBUG] IV (Base64): " + getIv());
            System.out.println("[DEBUG] Ciphertext (Base64): " + getMessage());

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, ivBytes);
            SecretKeySpec keySpec = new SecretKeySpec(sessionKey, "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            String decrypted = new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("[DEBUG] Decrypted payload: " + decrypted);

            return decrypted;
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to decrypt payload: " + e.getMessage());
            throw new RuntimeException("Failed to decrypt payload", e);
        }
    }

    @Override
    public JSONType toJSONType() {
        System.out.println("[DEBUG] Serializing PostMessage to JSON...");
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("user", user);
        obj.put("message", message);
        obj.put("wrappedkey", wrappedkey);
        obj.put("iv", iv);
        System.out.println("[DEBUG] Serialized PostMessage: " + obj.toString());
        return obj;
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        System.out.println("[DEBUG] Deserializing JSON to PostMessage...");
        if (!(obj instanceof JSONObject)) {
            throw new InvalidObjectException("Expected JSONObject.");
        }

        JSONObject json = (JSONObject) obj;
        this.type = json.getString("type");
        this.user = json.getString("user");
        this.message = json.getString("message");
        this.wrappedkey = json.getString("wrappedkey");
        this.iv = json.getString("iv");

        System.out.println("[DEBUG] Deserialized PostMessage: user=" + user + ", iv=" + iv);
    }

    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        System.out.println("[DEBUG] Decoding JSONObject to PostMessage...");
        PostMessage decoded = new PostMessage(
            obj.getString("user"),
            obj.getString("message"),
            obj.getString("wrappedkey"),
            obj.getString("iv")
        );
        decoded.type = obj.getString("type");
        return decoded;
    }
}
