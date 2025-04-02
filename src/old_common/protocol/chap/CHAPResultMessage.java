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
package old_common.protocol.chap;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import old_common.protocol.Message;

import java.io.InvalidObjectException;

/**
 * This class represents a CHAP result message.
 * @author Zach Kissel
 */
 public class CHAPResultMessage implements Message
 {
   private boolean isOK;  // The result of the protocol.
   private final String TYPE = "RFC1994 Result";


   /**
    * Constructs an empty result message, with the result of {@code false}
    */
    public CHAPResultMessage()
    {
      isOK = false;
    }

   /**
    * Construct a result message from a JSON object.
    * @param obj the JSON object representing a CHAP challenge message.
    * @throws InvalidObjectException if {@code obj} is not a valid
    * CHAP challenge message.
    */
   public CHAPResultMessage(JSONObject obj) throws InvalidObjectException
   {
     deserialize(obj);
   }

   /**
    * Construct a new response message that is OK if {@code isOK} is true and
    * NOK otherwise.
    * @param isOK true if CHAP was successful and false otherwise.
    */
   public CHAPResultMessage(boolean isOK)
   {
     this.isOK = isOK;
   }

   /**
    * If the result of the protocol was successful.
    * @return true if the protocol run was successful and false otherwise.
    */
    public boolean isSuccess()
    {
      return this.isOK;
    }

    /**
     * Gets the message type as a string.
     * @return the message type as a string.
     */
    public String getType()
    {
      return TYPE;
    }

   /**
    * Builds a new message from the given
    * JSON object if the message type matches.
    * @param obj the JSON object to decode.
    * @return the message built for the type.
    * @throws InvalidObjectException if {@code obj} is not of the
    * correct type.
    */
   public Message decode(JSONObject obj) throws InvalidObjectException
   {
    return new CHAPResultMessage(obj);
   }

    /**
     * Coverts json data to an object of this type.
     * @param obj a JSON type to deserialize.
     * @throws InvalidObjectException the type does not match this object.
     */
    public void deserialize(JSONType obj) throws InvalidObjectException
    {
      JSONObject msg;
      String[] keys = {"type", "result"};

      if (obj.isObject())
      {
        msg = (JSONObject) obj;
        msg.checkValidity(keys);
        
        // Validate the type.
        if (!msg.getString("type").equals(TYPE))
          throw new InvalidObjectException(
             "Invalid CHAP Message -- result message expected!");
        
        isOK = msg.getBoolean("result");
      }
      else
        throw new InvalidObjectException(
           "CHAP result message -- recieved array, expected Object.");
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     */
    public JSONType toJSONType()
    {
      JSONObject obj = new JSONObject();

      obj.put("type", TYPE);
      obj.put("result", isOK);

      return obj;
    }

    /**
     * Convert the object to a string.
     * @return a string representation of the object.
     */
     public String toString()
     {
       return "RFC1994 Result (" + isOK + ")";
     }
 }
