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

import merrimackutil.util.NonceCache;
import old_common.protocol.Message;
import old_common.protocol.Protocol;
import old_common.protocol.ProtocolChannel;
import old_common.protocol.ProtocolRole;
import old_common.protocol.ticket.Ticket;

import java.io.InvalidObjectException;
import java.security.SecureRandom;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.Cipher;
import java.util.Base64;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;


/**
 * This protocol between client and KDC to retrieve a ticket for communicating
 * with a service.
 * @author Zach Kissel
 */
 public class ServiceProto extends Protocol
 {

   /**
    * An enumeration for the state protocol state machine.
    */
    private enum  ServiceState
    {
      START,
      CLIENT_HELLO,
      SERVER_HELLO,
      CLIENT_RESP,
      ERROR,
      HANDSHAKE_COMPLETED
    }

   private ServiceState state;     // The state the protocol is in.
   private String service;          // The service requested.
   private String id;               // The id of the user requesting the service.
   private NonceCache nonceCache;  // Cache of used challenges.
   private boolean isInitialized;  // True if the protocol has been initialized.
   private Ticket ticket;           // The ticket.
   private byte[] sessionKey;              // The session key.
   private boolean protocolError;
   private String errorReason;
   private byte[] masterKey;
   private Cipher cipher;

   /**
    * Setup a new Service protocol object for the role {@code role}.
    * @param role the role client or server.
    * @param cache a nonce cache to work with.
    */
   public ServiceProto(ProtocolRole role, NonceCache cache)
   {
     super(role);

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

     // Setup the crypto tools.
     this.nonceCache = cache;

     // Mark the protocol as uninitialized.
     this.isInitialized = false;
     this.ticket = null;
     this.sessionKey = null;
     this.protocolError = false;
     this.errorReason = null;
     this.ticket = null;
     this.id = null;
     this.service = null;
   }

    /**
     * Gets the reason for the error.
     * @return the string representing the reason.
     */
    public String getReason()
    {
      return errorReason;
    }

    /**
     * Gets the session key.
     * @return the session key as a byte array.
     */
    public byte[] getSessionKey()
    {
      return sessionKey;
    }

    /**
     * Get the client id.
     * @return the client id as a string.
     */
     public String getClientID()
     {
       return id;
     }

     /**
      * Get the service name.
      * @return the service name as a string.
      */
    public String getServiceName()
    {
      return service;
    }

   /**
    * Initialize the client for communication.
    * @param service the requested service name.
    * @param id the name of the user requesting the ticket.
    * @param sessionKey the session key.
    * param ticket the ticket for the service.
    * @throws UnsupportedOperationException if called on a non-client.
    */
   public void initClient(String id, String service, byte[] sessionKey,
       Ticket ticket) throws UnsupportedOperationException
   {
     if (getRole() != ProtocolRole.CLIENT)
      throw new UnsupportedOperationException(
         "Init client can only be invoked on client.");

     this.state = ServiceState.START;
     this.service = service;
     this.id = id;
     this.ticket = ticket;
     this.isInitialized = true;
     this.sessionKey = sessionKey;
     this.masterKey = null;
     this.protocolError = false;
     this.errorReason = null;
   }

   /**
    * Initialize the server for communication.
    * @param secretStore the hash map storing all the secrets.
    * @param ticketLifeTime how long tickets should be good for in seconds.
    * @throws UnsupportedOperationException if called on a non-client.
    */
    public void initServer(String service, byte[] masterKey) throws
       UnsupportedOperationException
    {
      if (getRole() != ProtocolRole.SERVER)
        throw new UnsupportedOperationException(
           "Init server can only be invoked on server.");

      this.state = ServiceState.CLIENT_HELLO;
      this.isInitialized = true;
      this.service = service;
      this.masterKey = masterKey;
      this.protocolError = false;
      this.sessionKey = null;
      this.ticket = null;
      this.id = null;
      this.errorReason = null;
    }

   /**
    * Performs the next phase of the protocol based on the
    * message {@code msg} and role.
    * @param msg the protocol message from the peer.
    * @throws UnsupportedOperationException if the protocol has not been
    * initialized.
    * @throws IllegalArgumentException if the client has not recieved a message
    * and {@code msg} is not null.
    */
   public Message doPhase(Message msg) throws UnsupportedOperationException,
      IllegalArgumentException
   {
     if (!isInitialized)
      throw new UnsupportedOperationException("Protocol must be intialized.");

     // If the message is null and we are not in the START state we should
     // transition to the error state. This could happen if the server
     // disconnects permaturely, for exmaple.
     if (msg == null && state != ServiceState.START)
     {
       state = ServiceState.ERROR;
       return null;
     }

     // If we are in the error state, we can't continue.
     if (state == ServiceState.ERROR)
      return null;

     switch (state)
     {
       // When we are in the start state msg == null and
       // we must construct the initial message.
       case START:
         state = ServiceState.SERVER_HELLO;
         if (msg != null)
           throw new IllegalArgumentException("Expected no message");

         byte[] startNonce = nonceCache.getNonce();
         return new ClientHelloMessage(ticket,
             Base64.getEncoder().encodeToString(startNonce));


       // We are expecting a HELLo message from the client.
       case CLIENT_HELLO:
        state = ServiceState.CLIENT_RESP;
        ClientHelloMessage chello = (ClientHelloMessage) msg;

        if (!verifyAndExtractTicket(chello.getTicket()))
          return null;

        if (!checkAndUpdateNonceCache(chello.getNonce()))
          return null;

        return getHandshakeResponse(chello.getNonce(), service);

       // WE are expecting a hello from the server.
       case SERVER_HELLO:
        state = ServiceState.HANDSHAKE_COMPLETED;
        HandshakeChallengeResponseMessage shello =
           (HandshakeChallengeResponseMessage) msg;

        if (!checkAndUpdateNonceCache(shello.getNonce()))
          return null;

        if (!shello.getservice().equals(service))
        {
           state = ServiceState.ERROR;
           protocolError = true;
           errorReason = "Wrong prinicple expected: " + service;
           return null;
        }

        byte[] challenge = decryptNonce(sessionKey,
            Base64.getDecoder().decode(shello.getEncryptedResponse()),
            Base64.getDecoder().decode(shello.getIV()),
            shello.getservice());

        if (!nonceCache.containsNonce(challenge))
        {
          protocolError = true;
          errorReason = "Invalid response.";
          state = ServiceState.ERROR;
          return null;
        }
        return getHandshakeResponse(shello.getNonce(), id);


       case CLIENT_RESP:
         state = ServiceState.HANDSHAKE_COMPLETED;
         HandshakeChallengeResponseMessage shake =
            (HandshakeChallengeResponseMessage) msg;

         if (!checkAndUpdateNonceCache(shake.getNonce()))
           return null;

         if (!shake.getservice().equals(id))
         {
            state = ServiceState.ERROR;
            protocolError = true;
            errorReason = "Wrong prinicple expected: " + id;
            return null;
         }

        byte[] response = decryptNonce(sessionKey,
               Base64.getDecoder().decode(shake.getEncryptedResponse()),
               Base64.getDecoder().decode(shake.getIV()),
               shake.getservice());

       if (!nonceCache.containsNonce(response))
       {
         protocolError = true;
         errorReason = "Invalid response.";
         state = ServiceState.ERROR;
         return null;
       }

       // We have either reached an error state or the handshake
       // has been completed, we can't send any other messages.
       case ERROR:
       case HANDSHAKE_COMPLETED:
        return null;
     }
     return null;
   }

   /**
    * Perform the handshake.
    * @param channel the channel to perform the handshake on.
    * @return true if the handshake was successful; otherwise false.
    */
   public boolean doHandshake(ProtocolChannel channel)
   {
     if (!isInitialized)
      throw new UnsupportedOperationException("Protocol must be intialized.");

     // Load the messages needed by the protocol.
     channel.addMessageType(new ClientHelloMessage());
     channel.addMessageType(new HandshakeChallengeResponseMessage());
     channel.addMessageType(new ServiceMessage());

     // If we are the client perform the client flow.
     if (getRole() == ProtocolRole.CLIENT)
     {
       try
       {
         Message m1 = doPhase(null);
         channel.sendMessage(m1);
         Message m2 = channel.receiveMessage();
         Message m3 = doPhase(m2);
         if (state != ServiceState.ERROR)
          channel.sendMessage(m3);
       }
       catch (InvalidObjectException ex)
       {
        System.out.println(ex);
        return false;
       }
     }
     else   // We are the server perform that flow.
     {
       try
       {
         Message m1 = channel.receiveMessage();
         Message m2 = doPhase(m1);
         if (state != ServiceState.ERROR)
         {
          channel.sendMessage(m2);
          Message m3 = channel.receiveMessage();
          doPhase(m3);
         }
         else
         {
           return false;
         }
       }
       catch (InvalidObjectException ex)
       {
        return false;
       }
     }

     return !protocolError;
   }

   /**
    * Encrypts a nonce.
    * @param key the key used to encrypt the nonce.
    * @param nonce A byte array representation of the nonce.
    * @param iv the iv used for encryption.
    * @param id the identity of the sender.
    * @return the ciphertext as a byte array.
    */
    private byte[] encryptNonce(byte[] key, byte[] nonce, byte[] iv, String id)
    {
      try
      {
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
            new GCMParameterSpec(128, iv));
      }
      catch (InvalidKeyException | InvalidAlgorithmParameterException ex)
      {
        System.out.println(ex);
        return null;
      }
      try
      {
        cipher.updateAAD(id.getBytes(StandardCharsets.US_ASCII));
        return cipher.doFinal(nonce);
      }
      catch (IllegalBlockSizeException | BadPaddingException ex)
      {
        System.out.println(ex);
        return null;
      }
    }

    /**
     * Decrypts a nonce.
     * @param ctext A byte array representation of the encrypted nonce.
     * @param key the key used to encrypt the nonce.
     * @param iv the iv used for encryption.
     * @param id of the sender of the message.
     * @return the plaintext as a byte array.
     */
     private byte[] decryptNonce(byte[] key, byte[] ctext, byte[] iv, String id)
     {
       try
       {
         cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
            new GCMParameterSpec(128, iv));
       }
       catch (InvalidKeyException | InvalidAlgorithmParameterException ex)
       {
         System.out.println(ex);
         return null;
       }

       try
       {
         cipher.updateAAD(id.getBytes(StandardCharsets.US_ASCII));
         return cipher.doFinal(ctext);
       }
       catch (IllegalBlockSizeException | BadPaddingException ex)
       {
         System.out.println(ex);
         return null;
       }
     }

     /**
      * Constructs a new server hello message from the sepecified
      * nonce.
      * @param nonce the nonce to encrypt.
      */
     private HandshakeChallengeResponseMessage getHandshakeResponse(String nonce,
      String service)
     {
       SecureRandom rand = new SecureRandom();
       byte[] ivBytes = new byte[16];
       rand.nextBytes(ivBytes);
       byte[] encResp = encryptNonce(sessionKey,
          Base64.getDecoder().decode(nonce), ivBytes, service);
       return new HandshakeChallengeResponseMessage(service,
          Base64.getEncoder().encodeToString(encResp),
          Base64.getEncoder().encodeToString(nonceCache.getNonce()),
          Base64.getEncoder().encodeToString(ivBytes));
     }

     /**
      * Verifies the ticket and extracts the key.
      * @param ticket the ticket to verify.
      * @return true if the ticket verification was successful; otherwise, false.
      */
      private boolean verifyAndExtractTicket(Ticket ticket)
      {
        if (!ticket.getServiceName().equals(service))
        {
          state = ServiceState.ERROR;
          errorReason = "Ticket not for this service.";
          return false;
        }

        sessionKey = ticket.getSessionKey(masterKey);
        if (sessionKey == null)
        {
          state = ServiceState.ERROR;
          errorReason = "Can't extract session key, bad ticket.";
          return false;
        }
        id = ticket.getPrinciple();
        return true;
      }

    /**
     * Check and update the nonce cache.
     * @param nonce the noce to check.
     * @return true if the nonce verified and the cache was updated; otherwise,
     * false is returned.
     */
     private boolean checkAndUpdateNonceCache(String nonce)
     {
       if (nonceCache.containsNonce(Base64.getDecoder().decode(nonce)))
       {
         state = ServiceState.ERROR;
         errorReason = "Replayed message";
         return false;
       }
       nonceCache.addNonce(Base64.getDecoder().decode(nonce));
       return true;
     }
 }
