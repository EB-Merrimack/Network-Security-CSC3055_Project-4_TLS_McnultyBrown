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
 * This class represents a CHAP initial message.
 * @author Zach Kissel
 */
 public class CHAPInitialMessage implements Message
 {
   private String id;  // The id associated with the run.
   private final String TYPE = "RFC1994 Initial";

   /**
    * Constructs a new initial message with a {@code null} identity.
    */
   public CHAPInitialMessage()
   {
     id = null;
   }

   /**
    * Construct a initial message from a JSON object.
    * @param obj the JSON object representing a CHAP challenge message.
    * @throws InvalidObjectException if {@code obj} is not a valid
    * CHAP challenge message.
    */
   public CHAPInitialMessage(JSONObject obj) throws InvalidObjectException
   {
     deserialize(obj);
   }

   /**
    * Construct a new initial message with identity {@code id}.
    * @param id the identity of the user.
    */
   public CHAPInitialMessage(String id)
   {
     this.id = id;
   }

   /**
    * Get the identity from the message.
    * @return the identity associated with the message.
    */
    public String getIdentity()
    {
      return this.id;
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
    return new CHAPInitialMessage(obj);
   }

    /**
     * Coverts json data to an object of this type.
     * @param obj a JSON type to deserialize.
     * @throws InvalidObjectException the type does not match this object.
     */
    public void deserialize(JSONType obj) throws InvalidObjectException
    {
      JSONObject msg;
      String[] keys = {"type", "id"};
      if (obj.isObject())
      {
        msg = (JSONObject) obj;

        msg.checkValidity(keys);
        
        // Validate message type.
        if (!msg.getString("type").equals(TYPE))
          throw new InvalidObjectException(
             "Invalid CHAP Message -- initial message expected!");
        
        id = msg.getString("id");
      }
      else
        throw new InvalidObjectException(
           "CHAP initial message -- recieved array, expected Object.");
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     */
    public JSONType toJSONType()
    {
      JSONObject obj = new JSONObject();

      obj.put("type", TYPE);
      obj.put("id", id);

      return obj;
    }

    /**
     * Convert the object to a string.
     * @return a string representation of the object.
     */
     public String toString()
     {
       return "RFC1994 Initial (" + id + ")";
     }
 }
