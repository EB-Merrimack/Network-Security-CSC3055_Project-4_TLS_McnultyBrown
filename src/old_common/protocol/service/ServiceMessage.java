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
package old_common.protocol.service;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import old_common.protocol.Message;

import java.io.InvalidObjectException;

import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.util.Base64;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.InvalidAlgorithmParameterException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;

/**
 * This represents a service message.
 * @author Zach Kissel
 */
 public class ServiceMessage implements Message
 {
   private final String TYPE = "Service Message";
   private String encPayload;
   private String nonce;
   private String service;
   private String iv;
   private String id;
   /**
    * Constructs an empty ticket request with the service
    * name set to {@code null}
    */
    public ServiceMessage()
    {
      this.service = null;
      this.nonce = null;
      this.encPayload = null;
      this.iv = null;
      this.id = null;
    }

   /**
    * Constructs a new service message
    * @param service the name of the service.
    * @param
    */
    public ServiceMessage(String service, String id, String encPayload,
       String nonce, String iv)
    {
      this.service = service;
      this.nonce = nonce;
      this.encPayload = encPayload;
      this.iv = iv;
      this.id = id;
    }

    /**
     * Constructs a new service messagefrom
     * a JSON object.
     * @param obj the JSON object representing the request.
     * @throws InvalidObjectException if the object is not a ticket request.
     */
     public ServiceMessage(JSONObject obj) throws InvalidObjectException
     {
       deserialize(obj);
     }

   /**
    * Gets the client name from the message.
    * @return the client name
    */
   public String getClient()
   {
     return id;
   }

   /**
    * Gets the service name.
    * @return the service name.
    */
    public String getServiceName()
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
    * Get the encrypted payload from the message.
    * @return base64 encoded form of the encrypted payload.
    */
    public String getEncryptedPayload()
    {
      return encPayload;
    }

    public String getDecryptedPayload(byte[] sessionKey)
    {
      Cipher cipher = null;
      byte[] payload;

      try
      {
        cipher = Cipher.getInstance("AES/GCM/NoPadding");
      }
      catch (NoSuchAlgorithmException | NoSuchPaddingException ex)
      {
        System.out.println(
            "Fatal Error: AES/GCM/NoPadding algorithm not found.");
        System.out.println(ex);
        System.exit(1);
      }

      try
      {
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sessionKey, "AES"),
          new GCMParameterSpec(128, Base64.getDecoder().decode(iv)));
      }
      catch (InvalidKeyException | InvalidAlgorithmParameterException ex)
      {
        System.out.println(ex);
        return null;
      }

      try
      {
        cipher.updateAAD(id.getBytes(StandardCharsets.US_ASCII));
        cipher.updateAAD(service.getBytes(StandardCharsets.US_ASCII));
        cipher.updateAAD(Base64.getDecoder().decode(nonce));
        payload = cipher.doFinal(Base64.getDecoder().decode(encPayload));
      }
      catch (IllegalBlockSizeException | BadPaddingException ex)
      {
        System.out.println(ex);
        return null;
      }

      return new String(payload);
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
   return new ServiceMessage(obj);
  }

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject msg;
     String[] keys = {"type", "service", "nonce", "encrypted-payload", "iv", "id"};
     if (obj.isObject())
     {
       msg = (JSONObject) obj;

       msg.checkValidity(keys);
       
       // Validate type.
       if (!msg.getString("type").equals(TYPE))
         throw new InvalidObjectException(
            "Invalid Message -- service message expected!");
       
       service = msg.getString("service");
       nonce = msg.getString("nonce");
       encPayload = msg.getString("encrypted-payload");
       iv = msg.getString("iv");
       id = msg.getString("id");
     }
     else
       throw new InvalidObjectException(
          "service message response -- recieved array, expected Object.");
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
     obj.put("id", id);
     obj.put("encrypted-payload", encPayload);
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
      return "Service Message";
    }
 }
