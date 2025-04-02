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

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import old_common.protocol.Message;

import java.io.InvalidObjectException;

/**
 * This represents a ticket request message for the ticket protocol.
 * @author Zach Kissel
 */
 public class TicketResponseMessage implements Message
 {
   private final String TYPE = "Ticket Response";
   private SessionKey sessionKey; // The session key
   private Ticket ticket;     // The ticket;

   /**
    * Constructs an empty ticket response
    */
    public TicketResponseMessage()
    {
      sessionKey = null;
      ticket = null;
    }

   /**
    * Constructs a new ticket request message for
    * service {@code service}.
    */
    public TicketResponseMessage(SessionKey sessionKey, Ticket ticket)
    {
      this.sessionKey = sessionKey;
      this.ticket = ticket;
    }

    /**
     * Constructs a new ticket request message from
     * a JSON object.
     * @param obj the JSON object representing the request.
     * @throws InvalidObjectException if the object is not a ticket request.
     */
     public TicketResponseMessage(JSONObject obj) throws InvalidObjectException
     {
       deserialize(obj);
     }

   /**
    * Gets the ticket from the response.
    * @return the ticket.
    */
   public Ticket getTicket()
   {
     return ticket;
   }

   /**
    * Gets the session key from the response.
    * @return the ticket.
    */
    public SessionKey getSessionKey()
    {
      return sessionKey;
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
   return new TicketResponseMessage(obj);
  }

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject msg;
     String[] keys = {"type", "session-key", "ticket"};

     if (obj.isObject())
     {
       msg = (JSONObject) obj;

       msg.checkValidity(keys);
       
       // Validate type.
       if (!msg.getString("type").equals(TYPE))
         throw new InvalidObjectException(
            "Invalid Ticket Message -- response message expected!");
       
       sessionKey = new SessionKey(msg.getObject("session-key"));
       ticket = new Ticket(msg.getObject("ticket"));
     }
     else
       throw new InvalidObjectException(
          "Ticket request message -- recieved array, expected Object.");
   }

   /**
    * Converts the object to a JSON type.
    * @return a JSON type either JSONObject or JSONArray.
    */
   public JSONType toJSONType()
   {
     JSONObject obj = new JSONObject();

     obj.put("type", TYPE);
     obj.put("session-key", sessionKey.toJSONType());
     obj.put("ticket", ticket.toJSONType());

     return obj;
   }

   /**
    * Convert the object to a string.
    * @return a string representation of the object.
    */
    public String toString()
    {
      return "Ticket Response";
    }
 }
