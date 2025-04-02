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
package common.protocol.ticket;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;
import java.io.InvalidObjectException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import java.util.Base64;
import java.security.InvalidAlgorithmParameterException;
import java.nio.charset.StandardCharsets;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import javax.crypto.spec.GCMParameterSpec;

/**
 * This class reprsents a ticket for communication.
 * @author Zach Kissel
 */
public class Ticket implements JSONSerializable
{
  private byte[] sessionKey;    // The session key.
  private String encryptedSessionKey; // The base64 encoded session key.
  private long creationTime;    // The creation time in milliseconds.
  private long validityTime;    // The validity time of the ticket.
  private String id;            // The identity of the user;
  private String service;       // The service identity
  private String iv;            // The base 64 encoded iv;

  /**
   * Constructs a new empty ticket.
   * @param rootKey the raw root key.
   * @param sessionKey the bytes of the raw session key.
   * @param id the id of the user that requested the ticket.
   * @param service the id of the service that the ticket is shared with.
   * @param validityTime the duration from creation time for when the ticket
   * is valid.
   * @throws InvalidKeyException if the master key is invalid.
   * @throws IllegalBlockSizeException if the block is not of the correct size.
   * @throws BadPaddingException if the padding is invalid.
   */
  public Ticket(byte[] rootKey, byte[] sessionKey, String id,
     String service, long validityTime) throws InvalidKeyException,
     IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException
  {
    Cipher cipher = null;
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

    this.creationTime = System.currentTimeMillis();
    this.validityTime = validityTime;
    this.id = id;
    this.service = service;

    byte[] ivBytes = new byte[16];
    SecureRandom rand = new SecureRandom();
    rand.nextBytes(ivBytes);

    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(rootKey, "AES"),
        new GCMParameterSpec(128, ivBytes));
    cipher.updateAAD(toByteArray(creationTime));
    cipher.updateAAD(toByteArray(validityTime));
    cipher.updateAAD(id.getBytes(StandardCharsets.US_ASCII));
    cipher.updateAAD(service.getBytes(StandardCharsets.US_ASCII));
    encryptedSessionKey = Base64.getEncoder().encodeToString(
        cipher.doFinal(sessionKey));
    iv = Base64.getEncoder().encodeToString(ivBytes);

  }

  /**
   * Constructs a new ticket object from a JSON representation.
   * @param obj the JSON representation of the ticket object.
   * @param rootKey the master key.
   * @throws InvalidObjectException if the JSON object doesn't represent a
   * ticket.
   * @throws InvalidKeyException if the master key is invalid.
   * @throws IllegalBlockSizeException if the block is not of the correct size.
   * @throws BadPaddingException if the padding is invalid.
   * @throws InvalidAlgorithmParameterException if AES is not known.
   */
  public Ticket(JSONObject obj, byte[] rootKey) throws InvalidObjectException,
    InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
    InvalidAlgorithmParameterException
  {
    deserialize(obj);

    Cipher cipher = null;
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
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(rootKey, "AES"),
      new GCMParameterSpec(128, Base64.getDecoder().decode(iv)));
    cipher.updateAAD(toByteArray(creationTime));
    cipher.updateAAD(toByteArray(validityTime));
    cipher.updateAAD(id.getBytes(StandardCharsets.US_ASCII));
    cipher.updateAAD(service.getBytes(StandardCharsets.US_ASCII));
    sessionKey = cipher.doFinal(Base64.getDecoder().decode(encryptedSessionKey));
  }

  /**
   * Constructs a new ticket object from a JSON representation.
   * @param obj the JSON representation of the ticket object.
   */
  public Ticket(JSONObject obj) throws InvalidObjectException
  {
    deserialize(obj);
  }

  /**
   * Gets the session key from the ticket.
   * @return the session key null is returned if the session key is invalid
   * either expired or we are unable to decrypt the key.
   */
  public byte[] getSessionKey()
  {
    if ((creationTime + validityTime) > System.currentTimeMillis())
      return sessionKey;
    return null;
  }

  /**
   * Get the session key from the ticket only if the ticket is valid. 
   * @param rootKey the root key for the ticket.
   * @return the session key stored in the ticket.
   */
  public byte[] getSessionKey(byte[] rootKey)
  {
    Cipher cipher = null;
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
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(rootKey, "AES"),
        new GCMParameterSpec(128, Base64.getDecoder().decode(iv)));
    }
    catch (InvalidKeyException | InvalidAlgorithmParameterException ex)
    {
      System.out.println(ex);
      return null;
    }

    try
    {
      cipher.updateAAD(toByteArray(creationTime));
      cipher.updateAAD(toByteArray(validityTime));
      cipher.updateAAD(id.getBytes(StandardCharsets.US_ASCII));
      cipher.updateAAD(service.getBytes(StandardCharsets.US_ASCII));
      sessionKey = cipher.doFinal(Base64.getDecoder().decode(
         encryptedSessionKey));
    }
    catch (IllegalBlockSizeException | BadPaddingException ex)
    {
      System.out.println(ex);
      return null;
    }

    // Only return the session key if it is within the validity period.
    if ((creationTime + validityTime) > System.currentTimeMillis())
      return sessionKey;
    return null;
  }

  /**
   * Get the principle that sent the message.
   * @return the string representing the id of the user that sent the message.
   */
   public String getPrinciple()
   {
     return id;
   }

   /**
    * Get the service associated with the ticket.
    * @return the string representing the service.
    */
    public String getServiceName()
    {
      return service;
    }
    
   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject msg;
     String[] keys = {"encrypted-key", "creation-time", "validity-period", "id", "service", "iv"};
     if (obj.isObject())
     {
       msg = (JSONObject) obj;

       msg.checkValidity(keys);
       
       encryptedSessionKey = msg.getString("encrypted-key");
       creationTime = msg.getLong("creation-time");
       validityTime = msg.getLong("validity-period");
       id = msg.getString("id");
       service = msg.getString("service");
       iv = msg.getString("iv");
     }
     else
       throw new InvalidObjectException(
          "Recieved array, expected Object.");
   }

   /**
    * Converts the object to a JSON type.
    * @return a JSON type either JSONObject or JSONArray.
    */
   public JSONType toJSONType()
   {
     JSONObject obj = new JSONObject();

     obj.put("encrypted-key", encryptedSessionKey);
     obj.put("creation-time", creationTime);
     obj.put("validity-period", validityTime);
     obj.put("id", id);
     obj.put("service", service);
     obj.put("iv", iv);
     return obj;
   }

   /**
    * This method converts a long to an array of bytes.
    * @param buff the array of integers.
    * @return an array of bytes.
    */
    private byte[] toByteArray(long val)
    {
      ByteArrayOutputStream bao = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(bao);

      try
      {
        out.writeLong(val);
      }
      catch(IOException ioe)
      {
        // This will never happen, swallow the exception.
      }
      return bao.toByteArray();
    }
}
