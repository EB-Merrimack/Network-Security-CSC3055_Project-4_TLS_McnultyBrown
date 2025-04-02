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
 * This class represents a CHAP challenge message.
 * @author Zach Kissel
 */
 public class CHAPChallengeMessage implements Message
 {
   private byte[] challenge;  // The challenge associated with the run.
   private final String TYPE = "RFC1994 Challenge";

   /**
    * Constructs a new challenge message with a null challenge.
    */
    public CHAPChallengeMessage()
    {
      challenge = null;
    }

   /**
    * Construct a new challenge message from a JSON object.
    * @param obj the JSON object representing a CHAP challenge message.
    * @throws InvalidObjectException if {@code obj} is not a valid
    * CHAP challenge message.
    */
   public CHAPChallengeMessage(JSONObject obj) throws InvalidObjectException
   {
     deserialize(obj);
   }

   /**
    * Construct a new challenge message with challenge
    * {@code challenge}.
    * @param challenge the challenge.
    */
   public CHAPChallengeMessage(byte[] challenge)
   {
     this.challenge = challenge;
   }

   /**
    * Get the challenge from the message.
    * @return the array of bytes representing the challenge.
    */
    public byte[] getChallenge()
    {
      return this.challenge;
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
    return new CHAPChallengeMessage(obj);
   }

    /**
     * Coverts json data to an object of this type.
     * @param obj a JSON type to deserialize.
     * @throws InvalidObjectException the type does not match this object.
     */
    public void deserialize(JSONType obj) throws InvalidObjectException
    {
      JSONObject msg;
      String[] keys = {"type", "challenge"};

      if (obj.isObject())
      {
        msg = (JSONObject) obj;
        msg.checkValidity(keys);

        // Validate type of message. 
        if (!msg.getString("type").equals(TYPE))
          throw new InvalidObjectException(
             "Invalid CHAP Message -- challenge message expected!");
        
        challenge = Base64.getDecoder().decode(msg.getString("challenge"));
      }
      else
        throw new InvalidObjectException(
           "CHAP challenge message -- recieved array, expected Object.");
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     */
    public JSONType toJSONType()
    {
      JSONObject obj = new JSONObject();

      obj.put("type", TYPE);
      obj.put("challenge", Base64.getEncoder().encodeToString(challenge));

      return obj;
    }

    /**
     * Convert the object to a string.
     * @return a string representation of the object.
     */
     public String toString()
     {
       return "RFC1994 Challenge (" +
           Base64.getEncoder().encodeToString(challenge) + ")";
     }
 }
