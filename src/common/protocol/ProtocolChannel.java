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
package common.protocol;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.JsonIO;
import java.net.Socket;
import java.util.Scanner;
import common.protocol.messages.StatusMessage;
import java.io.PrintWriter;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.io.IOException;

/**
 * This class provides a communication channel for the protocol
 * it is a wrapper for a socket that has send and recieve messages
 *
 * @author Zach Kissel
 */
 public class ProtocolChannel
 {
    private Socket sock;        // The socket associated with the channel.
    private PrintWriter out;
    private Scanner in;
    private HashMap<String, Message> knownTypes;
    private boolean doTracing;

    /**
     * Construct a new wrapped socket.
     * @param sock an open and connected socket.
     * @throws IllegalArgumentException if the socket has not been connected
     * @throws IOException if I/O can't be performed on the socket.
     *
     */
    public ProtocolChannel(Socket sock) throws IllegalArgumentException, IOException
    {
      if (!sock.isConnected())
        throw new IllegalArgumentException("Socket must be connected.");
      this.sock = sock;
      out = new PrintWriter(sock.getOutputStream(), true);
      in = new Scanner(sock.getInputStream());
      knownTypes = new HashMap<>();
      doTracing = false;
      
      // Debug log for channel creation
      System.out.println("[DEBUG] ProtocolChannel created. Socket connected: " + sock.isConnected());
    }

    /**
     * Toggles tracing from off to on and on to off.
     */
    public void toggleTracing()
    {
      doTracing = !doTracing;
      System.out.println("[DEBUG] Tracing toggled. Current state: " + (doTracing ? "Enabled" : "Disabled"));
    }

   /**
    * Add a new known message type.
    * @param msg an instance of the new message type to add.
    */
    public void addMessageType(Message msg)
    {
      knownTypes.put(msg.getType(), msg);
      System.out.println("[DEBUG] Added new message type: " + msg.getType());
    }

   /**
    * Send a message to the other end of the channel.
    * @param msg the message to send.
    */
   public void sendMessage(Message msg)
   {
    trace("Local -> Remote: " + msg);
    
  
        JsonIO.writeSerializedObject(msg, out);
        System.out.println("[DEBUG] Sent message: " + msg);
   }

   /**
    * Receive a message from the other end of the
    * channel.
    *
    * @return the recieved message, null is returned if
    * the message can
    * @throws InvalidObjectException if the recieved message
    * can not be decoded.
    */
   public Message receiveMessage() throws InvalidObjectException
   {
    String raw = null;
    try {
        raw = in.nextLine();
    } catch (Exception ex) {
        System.out.println("[ERROR] Error while reading message: " + ex.getMessage());
    }

    if (raw == null) {
        System.out.println("[ERROR] No data received, raw message is null.");
        return null;
    }

    JSONObject obj = JsonIO.readObject(raw);
    Message m;
    if (knownTypes.containsKey(obj.getString("type"))) {
        m = knownTypes.get(obj.getString("type")).decode(obj);
        trace("Remote -> Local: " + m);
        return m;
    } else {
        System.out.println("[ERROR] Invalid message type: " + obj.getString("type"));
        throw new InvalidObjectException("Not a valid message.");
    }
   }

   /**
    *  Close the channel.
    */
   public void closeChannel()
   {
    try
    {
      sock.close();
      System.out.println("[DEBUG] Socket closed successfully.");
    }
    catch(IOException ex)
    {
      System.out.println("[ERROR] Failed to close socket: " + ex.getMessage());
    }
   }

   /**
    * Output message {@code msg} if tracing is enabled.
    * @param msg the message to display.
    */
   private void trace(String msg)
   {
    if (doTracing)
      System.out.println(msg);
   }

   public StatusMessage sendMessage(Class<StatusMessage> class1) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'sendMessage'");
   }
}
