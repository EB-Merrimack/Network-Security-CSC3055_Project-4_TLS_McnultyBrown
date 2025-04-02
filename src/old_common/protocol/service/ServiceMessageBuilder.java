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

import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.util.Base64;
import java.security.SecureRandom;
import merrimackutil.util.NonceCache;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;

/**
 * This class constructs new service messages based on a template provided
 * to the constructor.
 * @author Zach Kissel
 */
 public class ServiceMessageBuilder
 {

   private NonceCache cache;
   private String service;
   private String id;
   private byte[] sessionKey;
   private Cipher cipher;
   private SecureRandom rand;

   /**
    * Construct a new service message builder to construct messsages
    * with service name {@code service} and client id {@code id}.
    * @param service the service name.
    * @param id the client id.
    * @param cache the nonce cache.
    * @param sessionKey the session key associated with the builder.
    */
   public ServiceMessageBuilder(String service, String id, NonceCache cache,
      byte[] sessionKey)
   {
     this.service = service;
     this.id = id;
     this.cache = cache;
     this.sessionKey = sessionKey;

     cipher = null;
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

     rand = new SecureRandom();
   }

    /**
     * Constructs a new service message from plaintext payload {@code payload}.
     * @param msg the plaintext message to add to the message.
     * @return a new service message.
     */
    public ServiceMessage buildMessage(String msg)
    {
      byte[] ivBytes = new byte[16];
      rand.nextBytes(ivBytes);
      byte[] nonce = cache.getNonce();
      byte[] payload;

      try
      {
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sessionKey, "AES"),
          new GCMParameterSpec(128, ivBytes));
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
        cipher.updateAAD(nonce);
        payload = cipher.doFinal(msg.getBytes());
      }
      catch (IllegalBlockSizeException | BadPaddingException ex)
      {
        System.out.println(ex);
        return null;
      }

      return new ServiceMessage(service, id,
         Base64.getEncoder().encodeToString(payload),
         Base64.getEncoder().encodeToString(nonce),
         Base64.getEncoder().encodeToString(ivBytes));
    }
 }
