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
package client;

import java.net.Socket;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;

import merrimackutil.net.hostdb.HostsDatabase;

import java.io.InvalidObjectException;
import common.SecretStore;
import common.protocol.ProtocolChannel;
import common.protocol.ProtocolRole;
import common.protocol.chap.CHAPProto;
import common.protocol.service.ServiceMessage;
import common.protocol.service.ServiceMessageBuilder;
import common.protocol.service.ServiceProto;
import common.protocol.ticket.SessionKey;
import common.protocol.ticket.Ticket;
import common.protocol.ticket.TicketProto;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.util.NonceCache;
import merrimackutil.util.Tuple;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Console;
import java.util.Scanner;

/**
 * This file implements the KDC client.
 * @author Zach Kissel
 */
public class Old_KDCClient
{
  private static ProtocolChannel channel = null;
  private static String serviceName = null;
  private static String user = null;
  private static byte[] sessionKey = null;
  private static HostsDatabase hosts = null;
  private static String pass = null;
  private static NonceCache nonceCache;

  /**
   * Communicates with the echo service.
   * @param data the message to send to the echo service.
   * @returns false if the communication with the service failed.
   * @throws UnknownHostException if the host is unknown.
   * @throws IOException if IO can not be performed on the socket.
   */
  public static boolean runCommunication(String data, Ticket ticket) throws
     UnknownHostException, IOException
  {

    ServiceMessage msg = null;
    String payload = null;
    ServiceMessageBuilder msgBuilder = null;

    // We begin by running the four-way handshake with the service.
    channel = new ProtocolChannel(new Socket(hosts.getAddress(serviceName),
       hosts.getPort(serviceName)));

    ServiceProto handshake = new ServiceProto(ProtocolRole.CLIENT, nonceCache);
    handshake.initClient(user, serviceName, sessionKey, ticket);
    if (handshake.doHandshake(channel))
    {
      sessionKey = handshake.getSessionKey();
      msgBuilder = new ServiceMessageBuilder(handshake.getServiceName(),
         handshake.getClientID(), nonceCache, sessionKey);
     }
     else
     {
          channel.closeChannel();
          System.out.println(handshake.getReason());
          return false;
     }

    msg = msgBuilder.buildMessage(data);
    channel.sendMessage(msg);

    try
    {
      msg = (ServiceMessage) channel.receiveMessage();

      // Make sure the message is sane.
      if (nonceCache.containsNonce(
          Base64.getDecoder().decode(msg.getNonce())))
          return false;
      nonceCache.addNonce(Base64.getDecoder().decode(msg.getNonce()));

      if (!msg.getServiceName().equals(serviceName) ||
       !msg.getClient().equals(user))
         return false;

      payload = (msg.getDecryptedPayload(sessionKey));

      if (payload == null)
       return false;
     }
     catch (InvalidObjectException ex)
     {
       System.out.println(ex);
       return false;
     }
     System.out.println(payload);
     return true;
  }

  /**
   * Runs the protocol to get the ticket from the KDC.
   * @throws UnknownHostException if the host is unknown.
   * @throws IOException if there is a problem with socket IO.
   * @throws InvalidKeyException if there is a problem with key generation.
   */
  public static Ticket getTicket() throws UnknownHostException, IOException,
    InvalidKeyException
  {
    channel = new ProtocolChannel(new Socket(hosts.getAddress("kdcd"),
       hosts.getPort("kdcd")));

    CHAPProto chap = new CHAPProto(ProtocolRole.CLIENT, nonceCache);
    chap.initClient(user, pass);

    if (!chap.authenticate(channel))
    {
      System.out.println("Could not authenticate " + user + " to KDC.");
      channel.closeChannel();
      return null;
    }

    TicketProto tick = new TicketProto(ProtocolRole.CLIENT);
    tick.initClient(user, serviceName);

    if (tick.runTicketProto(channel))
    {
      Ticket ticket = tick.getTicket();

      SessionKey key = tick.getSessionKey();
      byte[] rootKey = SecretStore.deriveKey(user, pass);

      try
      {
        sessionKey = key.decodeKey(rootKey);
      }
      catch (IllegalBlockSizeException | BadPaddingException ex)
      {
        System.out.println("KDC Communication: internal error " + ex);
        channel.closeChannel();
        System.exit(1);
      }
      channel.closeChannel();
      return ticket;
    }

    System.out.println("Failed to get ticket.");
    channel.closeChannel();
    return null;
  }

  /**
   * Prints the help menu.
   */
   public static void usage()
   {
     System.out.println("usage:");
     System.out.println("  client --hosts <configfile> --user <user> --service <service>");
     System.out.println("  client --user <user> --service <service>");
     System.out.println("options:");
     System.out.println("  -h, --hosts\t\tSet the hosts file.");
     System.out.println("  -u, --user\t\tThe user name.");
     System.out.println("  -s, --service\t\tThe name of the service");
     System.exit(1);
   }


  /**
   * Loads the hosts information from the hosts file.
   * @param hostsName the name of the hosts file.
   */
  public static void loadHosts(String hostsName)
  {
    try
    {
      hosts = new HostsDatabase(new File(hostsName));
    }
    catch (FileNotFoundException ex)
    {
      System.out.println("Hosts file not found.");
      System.exit(1);
    }
    catch (InvalidObjectException ex)
    {
      System.out.println("Invalid hosts file.");
      System.exit(1);
    }
  }

  /**
   * Process the command line arguments.
   * @param args the array of command line arguments.
   */
  public static void processArgs(String[] args)
  {
    OptionParser parser;
    boolean doHosts = false;
    String hostsFile = null;

    LongOption[] opts = new LongOption[3];
    opts[0] = new LongOption("service", true, 's');
    opts[1] = new LongOption("user", true, 'u');
    opts[2] = new LongOption("hosts", true, 'h');

    Tuple<Character, String> currOpt;

    parser = new OptionParser(args);
    parser.setLongOpts(opts);
    parser.setOptString("h:u:s:");


    while (parser.getOptIdx() != args.length)
    {
      currOpt = parser.getLongOpt(false);

      switch (currOpt.getFirst())
      {
        case 'h':
          doHosts = true;
          hostsFile = currOpt.getSecond();
        break;
        case 's':
          serviceName = currOpt.getSecond();
        break;
        case 'u':
          user = currOpt.getSecond();
        break;
        case '?':
          usage();
        break;
      }
    }

    // If we don't have the necssary arguments abort.
    if (serviceName == null || user == null)
      usage();

    // Load the hosts file.
    if (doHosts)
      loadHosts(hostsFile);
    else
      loadHosts("hosts.json");

    if (!hosts.hostKnown(serviceName))
    {
      System.out.println("Host: " + serviceName + " unknown.");
      System.exit(1);
    }

    if (!hosts.hostKnown("kdcd"))
    {
      System.out.println("KDC host information unavailable.");
      System.exit(1);
    }
  }

  /**
   * This method prompts for a password and returns the password to the
   * caller.
   *
   * @return A non-empty password as a string.
   */
  public static String promptForPassword(String msg)
  {
    String passwd;
    Console cons = System.console();

    do
    {
      passwd = new String(cons.readPassword(msg + ": "));
    } while (passwd.isEmpty());

    return passwd;
  }

  public static void main(String[] args)
  {
    Ticket ticket = null;
    
    @SuppressWarnings("resource")
    Scanner scan = new Scanner(System.in);
  
    // Process the arguments so we know what we
    // are supposed to be doing.
    if (args.length < 1)
      usage();
    processArgs(args);

    // Prompt the user for the password.
    pass = promptForPassword("KDC Password");

    // Setup the nonce cache.
    nonceCache = new NonceCache(32, 30);

    try
    {
      ticket = getTicket();
    }
    catch (UnknownHostException ex)
    {
      System.out.println("KDC Communication: " + ex);
      channel.closeChannel();
      System.exit(1);
    }
    catch (IOException ioe)
    {
      System.out.println("KDC Communication: " + ioe.getMessage());
      System.exit(1);
    }
    catch (InvalidKeyException ike)
    {
      System.out.println("KDC Communication: " + ike);
      channel.closeChannel();
      System.exit(1);
    }

    if (ticket == null)
      return;

    try
    {
      System.out.print("Message to send: ");
      String message = scan.nextLine();

      if (!runCommunication(message, ticket))
        System.out.println("Communication with service failed.");
    }
    catch (UnknownHostException ex)
    {
      System.out.println("Service Communication: " + ex);
      channel.closeChannel();
    }
    catch (IOException ioe)
    {
      System.out.println("Service Communication: " + ioe);
      channel.closeChannel();
    }

  }
}
