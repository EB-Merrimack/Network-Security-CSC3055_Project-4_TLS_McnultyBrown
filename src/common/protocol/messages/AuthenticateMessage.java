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

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String getOtp() {
        return otp;
    }

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

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", "authenticate");
        obj.put("user", user);
        obj.put("pass", pass);
        obj.put("otp", otp);
        return obj;
    }

    @Override
    public String getType() {
        return "authenticate";
    }

    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        String user = obj.getString("user");
        String pass = obj.getString("pass");
        String otp = obj.getString("otp");
        return new AuthenticateMessage(user, pass, otp);
    }

    @Override
    public String toString() {
        return "[AuthenticateMessage] user=" + user + ", otp=" + otp;
    }
}