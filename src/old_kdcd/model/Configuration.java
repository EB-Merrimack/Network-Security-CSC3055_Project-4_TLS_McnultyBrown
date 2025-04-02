/* 
 * Copyright (C) 2023 - 2025  Zachary A. Kissel 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package old_kdcd.model;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;
import java.io.InvalidObjectException;

/**
 * This class represents the configuration data for the server.
 * @author Zach Kissel
 */
 public class Configuration implements JSONSerializable
 {
   private String secretsFile;
   private int port;
   private boolean doDebug;
   private long validityTime;

   /**
    * Constructs a configuration object from the appropriate JSON Object.
    * @param config the JSON formatted configuration object.
    * @throws InvalidObjectException if the config object is not valid.
    */
   public Configuration(JSONObject config) throws InvalidObjectException
   {
     deserialize(config);
   }

   /**
    * Gets the port number from the configuration file.
    * @return the port number the server should bind to.
    */
   public int getPort()
   {
     return this.port;
   }

   /**
    * Get the file associated with the secrets.
    * @return the string representing the secret file.
    */
    public String getSecretsFile()
    {
      return this.secretsFile;
    }

    /**
     * Check if debugging is turned on.
     * @return true if debugging is requested; otherwise, false.
     */
    public boolean doDebug()
    {
      return this.doDebug;
    }

    /**
     * Get the validity period for the tickets.
     * @param the validity period of a ticket in seconds.
     */
    public long getValidityPeriod()
    {
      return validityTime;
    }

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject config;
     String[] keys = {"secrets-file", "port", "validity-period", "debug"};
     if (obj.isObject())
     {
       config = (JSONObject) obj;
       
       config.checkValidity(keys);

       // Get the path to the secrets file.
       secretsFile = config.getString("secrets-file");

       // Get the port to bind to.
       port = config.getInt("port");

      // Validity time of granted tickets is required.
      validityTime = Long.parseLong(config.getString("validity-period"));

      // There is an option debug flag that turns on debugging.
      doDebug = config.getBoolean("debug");
     }
     else
       throw new InvalidObjectException(
          "Configuration -- recieved array, expected Object.");
   }

   /**
    * Converts the object to a JSON type.
    * @return a JSON type either JSONObject or JSONArray.
    */
   public JSONType toJSONType()
   {
     JSONObject obj = new JSONObject();

     obj.put("secrets-file", secretsFile);
     obj.put("port", port);
     obj.put("debug", doDebug);
     obj.put("validity-period", validityTime);

     return obj;
   }

 }
