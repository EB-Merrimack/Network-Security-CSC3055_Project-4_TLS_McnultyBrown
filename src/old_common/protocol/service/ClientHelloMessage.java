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
import old_common.protocol.ticket.Ticket;

import java.io.InvalidObjectException;

/**
 * This represents a client hello message
 * @author Zach Kissel
 */
 public class ClientHelloMessage implements Message
 {
   private final String TYPE = "Client Hello";
   private Ticket ticket;
   private String nonce;

   /**
    * Constructs an empty ticket request with the service
    * name set to {@code null}
    */
    public ClientHelloMessage()
    {
      this.ticket = null;
      this.nonce = null;
    }

   /**
    * Constructs a new ticket request message for
    * service {@code service}.
    * @param service the name of the service.
    */
    public ClientHelloMessage(Ticket ticket, String nonce)
    {
      this.ticket = ticket;
      this.nonce = nonce;
    }

    /**
     * Constructs a new client hello message from
     * a JSON object.
     * @param obj the JSON object representing the request.
     * @throws InvalidObjectException if the object is not a ticket request.
     */
     public ClientHelloMessage(JSONObject obj) throws InvalidObjectException
     {
       deserialize(obj);
     }

   /**
    * Gets the ticket from the message.
    * @return the ticket.
    */
   public Ticket getTicket()
   {
     return ticket;
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
   return new ClientHelloMessage(obj);
  }

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject msg;
     String[] keys = {"type", "ticket", "nonce"};

     if (obj.isObject())
     {
       msg = (JSONObject) obj;

       msg.checkValidity(keys);
       
       // Validate the type.
       if (!msg.getString("type").equals(TYPE))
         throw new InvalidObjectException(
            "Invalid Message -- client hello message expected!");
       
       ticket = new Ticket(msg.getObject("ticket"));
       nonce = msg.getString("nonce");
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
     obj.put("ticket", ticket.toJSONType());
     obj.put("nonce", nonce);
     return obj;
   }

   /**
    * Convert the object to a string.
    * @return a string representation of the object.
    */
    public String toString()
    {
      return "Client Hello";
    }
 }
