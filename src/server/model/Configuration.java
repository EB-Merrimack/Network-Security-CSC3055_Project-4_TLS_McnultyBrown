
package server.model;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;
import java.io.InvalidObjectException;

/**
 * This class represents the configuration data for the service.
 */
public class Configuration implements JSONSerializable {
    private int port;
    private boolean doDebug;
    private String usersFile;
    private String keystoreFile;
    private String keystorePass;
    private String boardFile;

    /**
     * Constructs a configuration object from the appropriate JSON Object.
     * @param config the JSON formatted configuration object.
     * @throws InvalidObjectException if the config object is not valid.
     */
    public Configuration(JSONObject config) throws InvalidObjectException {
        deserialize(config);
    }

    public int getPort() {
        return this.port;
    }

    public boolean doDebug() {
        return this.doDebug;
    }

    public String getUsersFile() {
        return usersFile;
    }

    public String getKeystoreFile() {
        return keystoreFile;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public String getBoardFile() {
        return boardFile;
    }

    /**
     * Converts JSON data to an object of this type.
     * @param obj a JSON type to deserialize.
     * @throws InvalidObjectException the type does not match this object.
     */
    public void deserialize(JSONType obj) throws InvalidObjectException {
        JSONObject config;
        String[] keys = {"users-file", "port", "keystore-file", "keystore-pass", "board-file", "debug"};
        if (obj.isObject()) {
            config = (JSONObject) obj;
            config.checkValidity(keys);
            
            usersFile = config.getString("users-file");
            port = config.getInt("port");
            keystoreFile = config.getString("keystore-file");
            keystorePass = config.getString("keystore-pass");
            boardFile = config.getString("board-file");
            doDebug = config.getBoolean("debug");
        } else {
            throw new InvalidObjectException("Configuration -- received array, expected Object.");
        }
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     */
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("users-file", usersFile);
        obj.put("port", port);
        obj.put("keystore-file", keystoreFile);
        obj.put("keystore-pass", keystorePass);
        obj.put("board-file", boardFile);
        obj.put("debug", doDebug);
        return obj;
    }
}
