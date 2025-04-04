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

import java.io.InvalidObjectException;

import common.protocol.Message;

/**
 * This represents a ticket request message for the ticket protocol.
 * @author Zach Kissel
 */
 public class TicketRequestMessage implements Message
 {
   private final String TYPE = "Ticket Request";
   private String service;
   private String id;

   /**
    * Constructs an empty ticket request with the service
    * name set to {@code null}
    */
    public TicketRequestMessage()
    {
      id = null;
      service = null;
    }

   /**
    * Constructs a new ticket request message for
    * service {@code service}.
    * @param service the name of the service.
    */
    public TicketRequestMessage(String id, String service)
    {
      this.id = id;
      this.service = service;
    }

    /**
     * Constructs a new ticket request message from
     * a JSON object.
     * @param obj the JSON object representing the request.
     * @throws InvalidObjectException if the object is not a ticket request.
     */
     public TicketRequestMessage(JSONObject obj) throws InvalidObjectException
     {
       deserialize(obj);
     }

   /**
    * Get the name of the service.
    * @return the name of the service as a string.
    */
    public String getServiceName()
    {
      return service;
    }

    /**
     * Get the name of the user.
     * @return the name of the user as a string.
     */
    public String getUserName()
    {
      return id;
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
   return new TicketRequestMessage(obj);
  }

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject msg;
     String[] keys = {"type", "service", "id"};

     if (obj.isObject())
     {
       msg = (JSONObject) obj;

       msg.checkValidity(keys);

       // Validate the type.
       if (!msg.getString("type").equals(TYPE))
         throw new InvalidObjectException(
            "Invalid Ticket Message -- initial message expected!");
       
       service = msg.getString("service");
       id = msg.getString("id");
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
     obj.put("service", service);
     obj.put("id", id);
     return obj;
   }

   /**
    * Convert the object to a string.
    * @return a string representation of the object.
    */
    public String toString()
    {
      return "Ticket Request (" + id + " -> " + service + ")";
    }
 }
