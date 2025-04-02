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
package old_common.protocol.ticket;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

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
import java.security.SecureRandom;
import javax.crypto.spec.GCMParameterSpec;

/**
 * This object represnents an encrypted session key.
 * @author Zach Kissel
 */
 public class SessionKey implements JSONSerializable
 {
   private String key;      // The base 64 encoded key.
   private String iv;
   private Cipher cipher;   // The cipher used to decrypt the session key.

   /**
    * Construct a new session key object from the master key and
    * provided session key.
    * @param masterKey the master key shared between the user and KDC.
    * @param sessionKey the new session key.
    * @throws InvalidKeyException if {@code masterKey} is a bad key.
    * @throws IllegalBlockSizeException if a bad block size is encountered.
    * @throws BadPaddingException if the padding mode is invalid.
    * @throws InvalidAlgorithmParameterException if the algorithm is bad.
    */
   public SessionKey(byte[] masterKey, byte[] sessionKey) throws InvalidKeyException,
       IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException
   {
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
     byte[] ivBytes = new byte[16];
     SecureRandom rand = new SecureRandom();
     rand.nextBytes(ivBytes);
     GCMParameterSpec gcmParams = new GCMParameterSpec(128, ivBytes);

     cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(masterKey, "AES"),
        gcmParams);
     iv = Base64.getEncoder().encodeToString(ivBytes);

     key = Base64.getEncoder().encodeToString(cipher.doFinal(sessionKey));

   }

   /**
    * Construct a new session key with {@code key} and {@code iv}.
    * @param key the base64 encoded session key.
    * @param iv the base64 encoded iv.
    */
   public SessionKey(String key, String iv)
   {
     this.key = key;
     this.iv = iv;

     try
     {
       cipher = Cipher.getInstance("AES/GCM/NoPadding");
     }
     catch (NoSuchAlgorithmException | NoSuchPaddingException ex)
     {
       System.out.println(
           "Fatal Error: AES/CBC/NoPadding algorithm not found.");
       System.out.println(ex);
       System.exit(1);
     }

   }

   /**
    * Constructs a new session key object from a JSON representation.
    * @param obj the JSON representation of the session key object.
    */
   public SessionKey(JSONObject obj) throws InvalidObjectException
   {
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
     deserialize(obj);
   }

   /**
    * Decodes the session key.
    * @param masterKey the master key shared between the user
    * and the KDC.
    * @return the bytes of the session key.
    * @throws InvalidKeyException if the master key is invalid.
    * @throws IllegalBlockSizeException if the block size is bad.
    * @throws BadPaddingException if the padding is incorrect.
    */
   public byte[] decodeKey(byte[] masterKey) throws InvalidKeyException,
     IllegalBlockSizeException, BadPaddingException
   {
     try
     {
       cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(masterKey, "AES"),
          new GCMParameterSpec(128, Base64.getDecoder().decode(iv)));
     }
     catch (InvalidAlgorithmParameterException ex)
     {
       System.out.println("Fatal Error: AES is an unknown algorithm");
       System.out.println(ex);
       System.exit(1);
     }
     return cipher.doFinal(Base64.getDecoder().decode(key));
   }

 

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject msg;
     String[] keys = {"encrypted-key", "iv"};
     if (obj.isObject())
     {
       msg = (JSONObject) obj;

       msg.checkValidity(keys);
      
       key = msg.getString("encrypted-key");
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

     obj.put("encrypted-key", key);
     obj.put("iv", iv);

     return obj;
   }
 }
