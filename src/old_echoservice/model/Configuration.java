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
package old_echoservice.model;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;
import java.io.InvalidObjectException;

/**
 * This class represents the configuration data for the service.
 * @author Zach Kissel
 */
 public class Configuration implements JSONSerializable
 {
   private int port;
   private boolean doDebug;
   private String serviceName;
   private String secret;

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
     * Check if debugging is turned on.
     * @return true if debugging is requested; otherwise, false.
     */
    public boolean doDebug()
    {
      return this.doDebug;
    }

  /**
   * Get the service name.
   * @return the service name as a string.
   */
   public String getServiceName()
   {
     return serviceName;
   }

   /**
    * Get the secret as a string.
    * @return the secret as a string.
    */
    public String getSecret()
    {
      return secret;
    }

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject config;
     String[] keys = {"port", "service-name", "service-secret", "debug"};
     if (obj.isObject())
     {
       config = (JSONObject) obj;

       config.checkValidity(keys);

       // Get the port to bind to.
       port = config.getInt("port");

      // Get the service name.
      serviceName = config.getString("service-name");

      // Get the service secret.
      secret = config.getString("service-secret");

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

     obj.put("port", port);
     obj.put("debug", doDebug);
     obj.put("service-name", serviceName);
     obj.put("service-secret", secret);

     return obj;
   }

 }
