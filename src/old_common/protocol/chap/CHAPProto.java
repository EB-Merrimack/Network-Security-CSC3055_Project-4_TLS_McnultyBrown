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

import merrimackutil.util.NonceCache;
import old_common.SecretStore;
import old_common.protocol.AuthenticationProtocol;
import old_common.protocol.Message;
import old_common.protocol.ProtocolChannel;
import old_common.protocol.ProtocolRole;

import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.InvalidObjectException;

/**
 * This provides an implementation of the CHAP protocol (RFC 1394). The
 * message format is, however, different.
 * @author Zach Kissel
 */
 public class CHAPProto extends AuthenticationProtocol
 {

   /**
    * An enumeration for the state protocol state machine.
    */
    private enum CHAPState
    {
      START,
      INITIAL,
      CHALLENGE,
      RESPONSE,
      RESULT,
      ERROR,
      COMPLETED
    }

   private CHAPState state;     // The state the protocol is in.
   private String id;          // The identity of the user.
   private String secret;      // The secret used
   private byte[] challenge;   // The challenge used in the challenge phase.
   private boolean result;    // The result of the protocol.
   private NonceCache challengeCache;  // Cache of used challenges.
   private MessageDigest hash;    // The instance of the hash function.
   private boolean isInitialized;  // True if the protocol has been initialized.
   private SecretStore secretStore;

   /**
    * Setup a new CHAP protocol object for the role {@code role}.
    * @param role the role client or server.
    * @param cache the nonce cache to use for the protocol.
    */
   public CHAPProto(ProtocolRole role, NonceCache cache)
   {
     super(role);

     // Setup the crypto tools.
     this.challengeCache = cache;
     try
     {
       hash = MessageDigest.getInstance("SHA256");
     }
     catch (NoSuchAlgorithmException ex)
     {
       System.out.println("Can't run CHAP protocol.");
       System.out.println(ex);
     }

     // Mark the protocol as uninitialized.
     this.isInitialized = false;
   }

   /**
    * Initialize the client for communication.
    * @param id the identity of the user.
    * @param secret the uers secret.
    * @throws UnsupportedOperationException if called on a non-client.
    */
   public void initClient(String id, String secret) throws UnsupportedOperationException
   {
     if (getRole() != ProtocolRole.CLIENT)
      throw new UnsupportedOperationException(
         "Init client can only be invoked on client.");

     this.state = CHAPState.START;
     this.secret = secret;
     this.id = id;
     this.isInitialized = true;
     this.challenge = null;
     this.result = false;
   }

   /**
    * Initialize the server for communication.
    * @param secretStore the hash map storing all the secrets.
    * @throws UnsupportedOperationException if called on a non-client.
    */
    public void initServer(SecretStore secretStore) throws
       UnsupportedOperationException
    {
      if (getRole() != ProtocolRole.SERVER)
        throw new UnsupportedOperationException(
           "Init server can only be invoked on server.");

      this.state = CHAPState.INITIAL;
      this.secretStore = secretStore;
      this.isInitialized = true;
      this.result = false;
      this.challenge = null;
    }


   /**
    * Get the result of the protocol
    * @return true if successful and false otherwise.
    * @throws UnsupportedOperationException if result is not avaliable because
    * the protocol is not completed.
    */
    public boolean getResult() throws UnsupportedOperationException
    {
      if (state != CHAPState.COMPLETED)
        throw new UnsupportedOperationException("Protocol not completed.");
      return result;
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
     if (msg == null && state != CHAPState.START)
     {
       state = CHAPState.ERROR;
       return null;
     }

     // If we are in the error state, we can't continue.
     if (state == CHAPState.ERROR)
      return null;

     // If we have encountered an error, enter the result state and
     // let that logic clean up the protocol.
     if (msg instanceof CHAPResultMessage && state != CHAPState.RESULT)
       state = CHAPState.ERROR;

     switch (state)
     {
       // When we are in the start state msg == null and
       // we must construct the initial message.
       case START:
        state = CHAPState.CHALLENGE;
        if (msg != null)
          throw new IllegalArgumentException("Expected no message");
        return new CHAPInitialMessage(id);

       // We are expecting an initial message, from which we
       // will generate a new challenge message.
       case INITIAL:
        state = CHAPState.RESPONSE;
        CHAPInitialMessage rmsg = (CHAPInitialMessage) msg;
        id = rmsg.getIdentity();

        // Lookup the secret. If we can't find it in the secret
        // store we should respond with a failed authentication
        // message.
        secret = secretStore.lookupSecret(id);
        if (secret == null)
        {
          state = CHAPState.ERROR;
          System.out.println("Could not lookup secret for user " + id);
          return new CHAPResultMessage(false);
        }

        getNextChallenge();
        return new CHAPChallengeMessage(challenge);


       // We are expecting a challenge message and will generate a new
       // response message.
       case CHALLENGE:
        state = CHAPState.RESULT;
        CHAPChallengeMessage cmsg = (CHAPChallengeMessage) msg;

        // If the challenge has been reused recently, we may
        // hava  problem.
        if (challengeCache.containsNonce(cmsg.getChallenge()))
        {
          state = CHAPState.ERROR;
          return null;
        }
        else
          challengeCache.addNonce(cmsg.getChallenge());

        return new CHAPResponseMessage(hashChallenge(cmsg.getChallenge()));

       // Handle the response to the challenge and send the appropriate
       // result message.
       case RESPONSE:
        state = CHAPState.COMPLETED;
        CHAPResponseMessage repmsg = (CHAPResponseMessage) msg;
        result = Arrays.equals(hashChallenge(challenge), repmsg.getHashValue());
        return new CHAPResultMessage(result);

       // We have received an error message.
       case ERROR:
        CHAPResultMessage emsg = (CHAPResultMessage) msg;
        result = emsg.isSuccess();
        return null;

       // Handle the message indicating the result of the protocol.
       case RESULT:
        state = CHAPState.COMPLETED;
        CHAPResultMessage resmsg = (CHAPResultMessage) msg;
        result = resmsg.isSuccess();
        return null;

       // This state means the protocol has been completed.
       // there is no next message to send.
       case COMPLETED:
        return null;
     }
     return null;
   }

   /**
    * Runs the authentication protocol using the specified {@code channel}
    * @param channel the channel to run the protocol over.
    * @return true if the user is authenticated; otherwise, false.
    */
   public boolean authenticate(ProtocolChannel channel)
   {
     if (!isInitialized)
      throw new UnsupportedOperationException("Protocol must be intialized.");

     // Load the messages needed by the protocol.
     channel.addMessageType(new CHAPResultMessage());
     channel.addMessageType(new CHAPInitialMessage());
     channel.addMessageType(new CHAPResponseMessage());
     channel.addMessageType(new CHAPChallengeMessage());

     // If we are the client perform the client flow.
     if (getRole() == ProtocolRole.CLIENT)
     {
       try
       {
         Message m1 = doPhase(null);
         channel.sendMessage(m1);

         // It's possible the the user name is unknown. In that
         // case the server will respond with an error message. It
         // Is also possible that the challenge message contained
         // a challenge message with a stale challenge. We will
         // not respond to stale challenges.
         Message m2 = channel.receiveMessage();
         Message m3 = doPhase(m2);
         if (state == CHAPState.ERROR)
          return false;

         channel.sendMessage(m3);
         Message m4 = channel.receiveMessage();
         doPhase(m4);
         return getResult();
       }
       catch (InvalidObjectException ex)
       {
        return false;
       }
     }
     else   // We are the server perform that flow.
     {
       try
       {
         Message m1 = channel.receiveMessage();

         Message m2 = doPhase(m1);
         channel.sendMessage(m2);

         if (state == CHAPState.ERROR)
          return false;

         Message m3 = channel.receiveMessage();
         Message m4 = doPhase(m3);
         channel.sendMessage(m4);
         return getResult();
       }
       catch (InvalidObjectException ex)
       {
        return false;
       }
     }
   }

   /**
    * Gets the next random challenge and adds it to a the
    * challenge cache.
    */
   private void getNextChallenge()
   {
     challenge = challengeCache.getNonce();
   }

   /**
    * Hashes the challenge message along with the secret
    * @return the hash value.
    */
    private byte[] hashChallenge(byte[] challenge)
    {
      hash.update(challenge);
      return hash.digest(secret.getBytes());
    }
 }
