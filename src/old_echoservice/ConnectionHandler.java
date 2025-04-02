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
package old_echoservice;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.Socket;
import java.util.Base64;

import common.SecretStore;
import common.protocol.ProtocolChannel;
import common.protocol.ProtocolRole;
import common.protocol.service.ServiceMessage;
import common.protocol.service.ServiceMessageBuilder;
import common.protocol.service.ServiceProto;
import merrimackutil.util.NonceCache;

public class ConnectionHandler implements Runnable {

    private ProtocolChannel channel;
    private NonceCache nonceCache;
    private boolean doDebug = false;
    private String serviceName;
    private String secret;
    private ServiceMessageBuilder msgBuilder = null;
    private byte[] sessionKey;
    private static String id = null;

    /**
     * Constructs a new connection handler for the given connection.
     * @param sock the socket to communicate over.
     * @param doDebug if tracing should be turned on or not.
     * @param serviceName the name of the service.
     * @param secret the secret.
     * @param nonceCache the nonce cache of the daemon.
     * @throws IllegalArgumentException the socket is invalid.
     * @throws IOException we can't read or write from the channel.
     */
    public ConnectionHandler(Socket sock, boolean doDebug, String serviceName, String secret, NonceCache nonceCache) throws IllegalArgumentException, IOException
    {
        this.channel = new ProtocolChannel(sock);
        this.nonceCache = nonceCache;
        this.serviceName = serviceName;
        this.secret = secret;
    }

    /**
     * Handles the echo service connection.
     */
    @Override
    public void run() {
    
        // Check if we should do tracing.
        if (doDebug)
          channel.toggleTracing();

        ServiceProto handshake = new ServiceProto(ProtocolRole.SERVER, nonceCache);

        handshake.initServer(serviceName,
           SecretStore.deriveKey(serviceName, secret));

        if (handshake.doHandshake(channel))
        {
            sessionKey = handshake.getSessionKey();
            id = handshake.getClientID();

            msgBuilder = new ServiceMessageBuilder(serviceName,
               handshake.getClientID(), nonceCache, sessionKey);
        }
        else
          channel.closeChannel();

        runCommunication();
        channel.closeChannel();
      }

      /**
       * Run the communication between the service and the client after the handshake.
       */
      private void runCommunication() {

          ServiceMessage msg = null;
          String payload = null;

          try {
              msg = (ServiceMessage) channel.receiveMessage();

              // Make sure the message is sane.
              if (nonceCache.containsNonce(
                      Base64.getDecoder().decode(msg.getNonce())))
                  return;
              nonceCache.addNonce(Base64.getDecoder().decode(msg.getNonce()));

              if (!msg.getServiceName().equals(serviceName) ||
                      !msg.getClient().equals(id))
                  return;

              payload = msg.getDecryptedPayload(sessionKey);

              if (payload == null)
                  return;
          } catch (InvalidObjectException ex) {
              System.out.println(ex);
              return;
          }
          channel.sendMessage(msgBuilder.buildMessage(payload.toUpperCase()));
      }
    
}
