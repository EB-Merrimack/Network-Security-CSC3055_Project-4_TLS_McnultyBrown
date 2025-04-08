package common.protocol.messages;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Represents a single encrypted post message.
 */
public class PostMessage extends Post {
    private String recipient;
    private String ciphertext;
    private String encryptedKey;
    private String iv;

    public PostMessage(String user, String message, String wrappedKey, String iv, 
                       String recipient, String ciphertext, String encryptedKey) {
        super(user, message, wrappedKey, iv);  // Pass parameters to the superclass (Post)
        this.recipient = recipient;
        this.ciphertext = ciphertext;
        this.encryptedKey = encryptedKey;
        this.iv = iv;
    }

    // Getters
    public String getRecipient() {
        return recipient;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getEncryptedKey() {
        return encryptedKey;
    }

    public String getIv() {
        return iv;
    }

    // Deserialize from JSON
    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!obj.isObject()) {
            throw new InvalidObjectException("Expected JSONObject for PostMessage.");
        }

        JSONObject jsonObject = (JSONObject) obj;

        // Deserialize Post fields (assuming Post implements JSONSerializable)
        super.deserialize(jsonObject);

        // Deserialize fields specific to PostMessage
        this.recipient = jsonObject.getString("recipient");
        this.ciphertext = jsonObject.getString("ciphertext");
        this.encryptedKey = jsonObject.getString("encryptedKey");
        this.iv = jsonObject.getString("iv");
    }

    // Serialize the PostMessage object to JSON
    @Override
    public JSONType toJSONType() {
        // Serialize Post fields (assuming Post implements toJSONType)
        JSONObject jsonObject = (JSONObject) super.toJSONType();

        // Add PostMessage specific fields
        jsonObject.put("recipient", recipient);
        jsonObject.put("ciphertext", ciphertext);
        jsonObject.put("encryptedKey", encryptedKey);
        jsonObject.put("iv", iv);

        return jsonObject;
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
}
