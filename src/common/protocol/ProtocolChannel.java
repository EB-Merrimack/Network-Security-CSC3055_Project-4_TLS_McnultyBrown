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
    }

    /**
     * Toggles tracing from off to on and on to off.
     */
    public void toggleTracing()
    {
      doTracing = !doTracing;
    }

   /**
    * Add a new known message type.
    * @param msg an instance of the new message type to add.
    */
    public void addMessageType(Message msg)
    {
      knownTypes.put(msg.getType(), msg);
    }

   /**
    * Send a message to the other end of the channel.
    * @param msg the message to send.
    */
   public void sendMessage(Message msg)
   {
    trace("Local -> Remote: " + msg);
    JsonIO.writeSerializedObject(msg, out);
    out.println();
    out.flush();
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
    String raw = in.nextLine();
    JSONObject obj = JsonIO.readObject(raw);
    Message m;
    if (knownTypes.containsKey(obj.getString("type")))
      m = knownTypes.get(obj.getString("type")).decode(obj);
    else
      throw new InvalidObjectException("Not a valid message.");

    trace("Remote -> Local: " + m);
    return m;
   }

   /**
    *  Close the channel.
    */
   public void closeChannel()
   {
    try
    {
      sock.close();
    }
    catch(IOException ex)
    {
      // Swallow this exception, if the socket can't be closed.
      // it's not a problem for us.
    }
   }

   /**
    * Output message {@code msg} is tracing is enabled.
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
