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
package common.protocol.service;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import java.io.InvalidObjectException;

import common.protocol.Message;

/**
 * This represents a challenge response message
 * @author Zach Kissel
 */
 public class HandshakeChallengeResponseMessage implements Message
 {
   private final String TYPE = "Handshake Challenge Response";
   private String encResp;
   private String nonce;
   private String service;
   private String iv;

   /**
    * Constructs an empty ticket request with the service
    * name set to {@code null}
    */
    public HandshakeChallengeResponseMessage()
    {
      this.service = null;
      this.nonce = null;
      this.encResp = null;
      this.iv = null;
    }

   /**
    * Constructs a new ticket request message for
    * service {@code service}.
    * @param service the name of the service.
    */
    public HandshakeChallengeResponseMessage(String service, String encResp,
       String nonce, String iv)
    {
      this.service = service;
      this.encResp = encResp;
      this.nonce = nonce;
      this.iv = iv;
    }

    /**
     * Constructs a new client hello message from
     * a JSON object.
     * @param obj the JSON object representing the request.
     * @throws InvalidObjectException if the object is not a ticket request.
     */
     public HandshakeChallengeResponseMessage(JSONObject obj) throws
        InvalidObjectException
     {
       deserialize(obj);
     }

   /**
    * Gets the service name from the message.
    * @return the service name
    */
   public String getservice()
   {
     return service;
   }

   /**
    * Get the nonce from the message.
    * @return base64 encoded form of the nonce.
    */
    public String getNonce()
    {
      return nonce;
    }

   /**
    * Get the IV from the message.
    * @return base64 encoded form of the IV.
    */
    public String getIV()
    {
      return iv;
    }

   /**
    * Get the encrypted response from the message.
    * @return base64 encoded form of the encrypted response.
    */
    public String getEncryptedResponse()
    {
      return encResp;
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
   return new HandshakeChallengeResponseMessage(obj);
  }

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject msg;
     String[] keys = {"type", "service", "nonce", "encrypted-response", "iv"};
     if (obj.isObject())
     {
       msg = (JSONObject) obj;
      
       msg.checkValidity(keys);
       
       // Validate the type.
       if (!msg.getString("type").equals(TYPE))
         throw new InvalidObjectException(
            "Invalid Message -- handshake challenge response message expected!");
       
       service = msg.getString("service");
       nonce = msg.getString("nonce");
       encResp = msg.getString("encrypted-response");
       iv = msg.getString("iv");
     }
     else
       throw new InvalidObjectException(
          "Handshake challenge response -- recieved array, expected Object.");
   }

   /**
    * Converts the object to a JSON type.
    * @return a JSON type either JSONObject or JSONArray.
    */
   public JSONType toJSONType()
   {
     JSONObject obj = new JSONObject();

     obj.put("type", TYPE);
     obj.put("service", service);
     obj.put("encrypted-response", encResp);
     obj.put("nonce", nonce);
     obj.put("iv", iv);
     return obj;
   }

   /**
    * Convert the object to a string.
    * @return a string representation of the object.
    */
    public String toString()
    {
      return "Handshake Challenge Response";
    }
 }
