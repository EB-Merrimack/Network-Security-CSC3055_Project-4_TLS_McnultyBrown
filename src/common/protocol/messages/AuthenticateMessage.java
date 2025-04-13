package common.protocol.messages;

import merrimackutil.json.types.*;
import common.protocol.Message;

import java.io.InvalidObjectException;

/**
 * A message used to authenticate a user with a username, password, and OTP.
 */
public class AuthenticateMessage implements Message {

    private String user;
    private String pass;
    private String otp;

    // Empty constructor for deserialization
    public AuthenticateMessage() {}

    // Constructor for sending
    public AuthenticateMessage(String user, String pass, String otp) {
        this.user = user;
        this.pass = pass;
        this.otp = otp;
    }

    /**
     * Returns the username associated with this authentication message.
     * @return the username associated with this authentication message
     */
    public String getUser() {
        return user;
    }

/**
 * Returns the password associated with this authentication message.
 * @return the password associated with this authentication message
 */

    public String getPass() {
        return pass;
    }

    /**
     * Returns the one-time password associated with this authentication message.
     * @return the one-time password associated with this authentication message
     */
    public String getOtp() {
        return otp;
    }

/**
 * Deserializes a JSON object into an AuthenticateMessage instance.
 *
 * @param obj the JSON object to deserialize
 * @throws InvalidObjectException if the object is not a valid JSONObject
 */

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!(obj instanceof JSONObject)) {
            throw new InvalidObjectException("Expected JSONObject");
        }

        JSONObject json = (JSONObject) obj;
        this.user = json.getString("user");
        this.pass = json.getString("pass");
        this.otp = json.getString("otp");

     
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     */
    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", "authenticate");
        obj.put("user", user);
        obj.put("pass", pass);
        obj.put("otp", otp);


        return obj;
    }

    /**
     * Gets the message type as a string.
     * @return the message type as a string.
     */
    @Override
    public String getType() {
    
        return "authenticate";
    }

    /**
     * Decodes a JSON object into an AuthenticateMessage instance.
     *
     * @param obj the JSON object to decode
     * @return an AuthenticateMessage instance
     * @throws InvalidObjectException if the object is not a valid JSONObject
     */
    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        String user = obj.getString("user");
        String pass = obj.getString("pass");
        String otp = obj.getString("otp");

       
        return new AuthenticateMessage(user, pass, otp);
    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "[AuthenticateMessage] user=" + user + ", otp=" + otp;
    }
}
