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

import java.util.Base64;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import old_common.protocol.Message;

import java.io.InvalidObjectException;

/**
 * This class represents a CHAP response message.
 * @author Zach Kissel
 */
 public class CHAPResponseMessage implements Message
 {
   private byte[] hashVal;  // The hash value of the response..
   private final String TYPE = "RFC1994 Response";

   /**
    * Constructs an empty response message with a {@code null} has value.
    */
    public CHAPResponseMessage()
    {
      hashVal = null;
    }

   /**
    * Construct a response message from a JSON object.
    * @param obj the JSON object representing a CHAP challenge message.
    * @throws InvalidObjectException if {@code obj} is not a valid
    * CHAP challenge message.
    */
   public CHAPResponseMessage(JSONObject obj) throws InvalidObjectException
   {
     deserialize(obj);
   }

   /**
    * Construct a new initial message with identity {@code id}.
    * @param the hash value associated with the message.
    */
   public CHAPResponseMessage(byte[] hashVal)
   {
     this.hashVal = hashVal;
   }

   /**
    * Get the hash value from  the message.
    * @return the hash value as an array of bytes.
    */
    public byte[] getHashValue()
    {
      return this.hashVal;
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
    return new CHAPResponseMessage(obj);
   }

    /**
     * Coverts json data to an object of this type.
     * @param obj a JSON type to deserialize.
     * @throws InvalidObjectException the type does not match this object.
     */
    public void deserialize(JSONType obj) throws InvalidObjectException
    {
      JSONObject msg;
      String[] keys = {"type", "hash"};

      if (obj.isObject())
      {
        msg = (JSONObject) obj;

        msg.checkValidity(keys);
        
        // Validate the type.
        if (!msg.getString("type").equals(TYPE))
          throw new InvalidObjectException(
             "Invalid CHAP Message -- response message expected!");
        
        hashVal = Base64.getDecoder().decode(msg.getString("hash"));
      }
      else
        throw new InvalidObjectException(
           "CHAP response message -- recieved array, expected Object.");
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     */
    public JSONType toJSONType()
    {
      JSONObject obj = new JSONObject();

      obj.put("type", TYPE);
      obj.put("hash", Base64.getEncoder().encodeToString(hashVal));

      return obj;
    }

    /**
     * Convert the object to a string.
     * @return a string representation of the object.
     */
     public String toString()
     {
       return "RFC1994 Response (" +
          Base64.getEncoder().encodeToString(hashVal) + ")";
     }
 }
