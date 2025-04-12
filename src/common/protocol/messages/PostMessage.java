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
    private String User;        // recipient username
    private String ciphertext;   // base64 AES ciphertext
    private String wrappedKey;   // base64 ElGamal-wrapped AES key
    private String iv;           // base64 AES IV

    public PostMessage() {}

    public PostMessage(String user, String ciphertext, String wrappedKey, String iv) {
        this.User = user;
        this.ciphertext = ciphertext;
        this.wrappedKey = wrappedKey;
        this.iv = iv;
    }

    public String getUser() {
        return User;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getWrappedKey() {
        return wrappedKey;
    }

    public String getIv() {
        return iv;
    }
    
    public String getDecryptedPayload(byte[] sessionKey) {
        try {
            byte[] ivBytes = Base64.getDecoder().decode(getIv());
            byte[] cipherBytes = Base64.getDecoder().decode(getCiphertext());

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
        obj.put("User", User);
        obj.put("ciphertext", ciphertext);
        obj.put("wrappedKey", wrappedKey);
        obj.put("iv", iv);
        return obj;
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!(obj instanceof JSONObject)) {
            throw new InvalidObjectException("Expected JSONObject.");
        }

        JSONObject json = (JSONObject) obj;
        this.User = json.getString("User");
        this.ciphertext = json.getString("ciphertext");
        this.wrappedKey = json.getString("wrappedKey");
        this.iv = json.getString("iv");
    }

    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        return new PostMessage(
            obj.getString("recvr"),
            obj.getString("ciphertext"),
            obj.getString("wrappedKey"),
            obj.getString("iv")
        );
    }

    @Override
    public String toString() {
        return "PostMessage{" +
            "User='" + User + '\'' +
            ", ciphertext='" + ciphertext + '\'' +
            ", wrappedKey='" + wrappedKey + '\'' +
            ", iv='" + iv + '\'' +
            '}';
    }
}
