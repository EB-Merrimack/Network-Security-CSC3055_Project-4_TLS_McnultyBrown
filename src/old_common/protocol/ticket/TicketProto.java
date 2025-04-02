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

import java.io.InvalidObjectException;
import java.security.SecureRandom;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;

import common.SecretStore;
import common.protocol.Message;
import common.protocol.Protocol;
import common.protocol.ProtocolChannel;
import common.protocol.ProtocolRole;

import javax.crypto.BadPaddingException;
import java.security.InvalidAlgorithmParameterException;

/**
 * This protocol between client and KDC to retrieve a ticket for communicating
 * with a service.
 * @author Zach Kissel
 */
 public class TicketProto extends Protocol
 {

   /**
    * An enumeration for the state protocol state machine.
    */
    private enum TicketState
    {
      START,
      REQUEST,
      RESPONSE,
      ERROR,
      COMPLETED
    }

   private TicketState state;     // The state the protocol is in.
   private String service;          // The service requested.
   private String id;               // The id of the user requesting the service.
   private boolean isInitialized;  // True if the protocol has been initialized.
   private SecretStore secretStore;
   private Ticket ticket;           // The ticket.
   private SessionKey key;              // The session key.
   private long ticketLifeTime;
   private boolean protocolError;
   private String errorReason;

   /**
    * Setup a new CHAP protocol object for the role {@code role}.
    * @param role the role client or server.
    */
   public TicketProto(ProtocolRole role)
   {
     super(role);

     // Mark the protocol as uninitialized.
     this.isInitialized = false;
     this.ticket = null;
     this.key = null;
     this.protocolError = false;
     this.errorReason = null;
     this.secretStore = null;
     this.ticket = null;
     this.id = null;
     this.service = null;
   }

   /**
    * Get the ticket.
    * @return the ticket.
    */
    public Ticket getTicket()
    {
      return this.ticket;
    }

    /**
     * Get the key for communication with the peer.
     * @return the session key
     */
    public SessionKey getSessionKey()
    {
      return this.key;
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
    * Initialize the client for communication.
    * @param service the requested service name.
    * @param id the name of the user requesting the ticket.
    * @throws UnsupportedOperationException if called on a non-client.
    */
   public void initClient(String id, String service) throws UnsupportedOperationException
   {
     if (getRole() != ProtocolRole.CLIENT)
      throw new UnsupportedOperationException(
         "Init client can only be invoked on client.");

     this.state = TicketState.START;
     this.service = service;
     this.id = id;
     this.isInitialized = true;
     this.key = null;
     this.protocolError = false;
     this.errorReason = null;
     this.ticket = null;
     this.secretStore = null;
   }

   /**
    * Initialize the server for communication.
    * @param secretStore the hash map storing all the secrets.
    * @param ticketLifeTime how long tickets should be good for in seconds.
    * @throws UnsupportedOperationException if called on a non-client.
    */
    public void initServer(SecretStore secretStore, long ticketLifeTime) throws
       UnsupportedOperationException
    {
      if (getRole() != ProtocolRole.SERVER)
        throw new UnsupportedOperationException(
           "Init server can only be invoked on server.");

      this.state = TicketState.REQUEST;
      this.secretStore = secretStore;
      this.isInitialized = true;
      this.key = null;
      this.ticket = null;
      this.ticketLifeTime = ticketLifeTime * 1000;
      this.service = null;
      this.id = null;
      this.protocolError = false;
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
     if (msg == null && state != TicketState.START)
     {
       state = TicketState.ERROR;
       return null;
     }

     // If we are in the error state, we can't continue.
     if (state == TicketState.ERROR)
      return null;

    // Handle the case when we recieve an out of band error.
    if (msg instanceof TicketErrorMessage && state != TicketState.ERROR)
      state = TicketState.ERROR;

     switch (state)
     {
       // When we are in the start state msg == null and
       // we must construct the initial message.
       case START:
        state = TicketState.RESPONSE;
        if (msg != null)
          throw new IllegalArgumentException("Expected no message");
        return new TicketRequestMessage(id, service);

       // We are expecting an request message, from which we
       // will generate a new ticket ticket response
       case REQUEST:
        state = TicketState.COMPLETED;
        TicketRequestMessage req = (TicketRequestMessage) msg;

        // Get a random session key.
        SecureRandom rand = new SecureRandom();
        byte[] sessionKey = new byte[16];
        rand.nextBytes(sessionKey);

        // Build the Session key object
        byte[] idKey = secretStore.deriveKey(req.getUserName());
        byte[] serviceKey = secretStore.deriveKey(req.getServiceName());

        if (idKey == null)
        {
          errorReason = "User " + req.getUserName() + " unknown.";
          protocolError = true;
          state = TicketState.ERROR;
          return new TicketErrorMessage(errorReason);
        }

        if (serviceKey == null)
        {
          errorReason = "Service " + req.getServiceName() + " unknown.";
          protocolError = true;
          state = TicketState.ERROR;
          return new TicketErrorMessage(errorReason);
        }

        try
        {
          ticket = new Ticket(serviceKey, sessionKey, req.getUserName(),
            req.getServiceName(), ticketLifeTime);
          key = new SessionKey(idKey, sessionKey);
        }
        catch (InvalidKeyException | IllegalBlockSizeException |
           BadPaddingException | InvalidAlgorithmParameterException ex)
        {
           state = TicketState.ERROR;
           errorReason = "Internal crypto error.";
           protocolError = true;
           return new TicketErrorMessage("Internal crypto error.");

        }
        return new TicketResponseMessage(key, ticket);

       // We are expecting a ticket response message
       case RESPONSE:
        state = TicketState.COMPLETED;
        TicketResponseMessage tresp = (TicketResponseMessage) msg;
        ticket = tresp.getTicket();
        key = tresp.getSessionKey();
        return null;

       // We have received an error message.
       case ERROR:
        TicketErrorMessage tickErr = (TicketErrorMessage) msg;
        protocolError = true;
        errorReason = tickErr.getReason();
        return null;

       // This state means the protocol has been completed.
       // there is no next message to send.
       case COMPLETED:
        return null;
     }
     return null;
   }

   /**
    * Runs the ticket protocol using the specified {@code channel}
    * @param channel the channel to run the protocol over.
    * @return true if ticket protocol was successful; otherwise, false.
    */
   public boolean runTicketProto(ProtocolChannel channel)
   {
     if (!isInitialized)
      throw new UnsupportedOperationException("Protocol must be intialized.");

     // Load the messages needed by the protocol.
     channel.addMessageType(new TicketRequestMessage());
     channel.addMessageType(new TicketResponseMessage());
     channel.addMessageType(new TicketErrorMessage());

     // If we are the client perform the client flow.
     if (getRole() == ProtocolRole.CLIENT)
     {
       try
       {
         Message m1 = doPhase(null);
         channel.sendMessage(m1);
         Message m2 = channel.receiveMessage();
         doPhase(m2);
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
         channel.sendMessage(m2);
       }
       catch (InvalidObjectException ex)
       {
        return false;
       }
     }

     return !protocolError;
   }

 }
