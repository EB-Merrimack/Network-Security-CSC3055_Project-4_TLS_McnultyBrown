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
package kdcd;

import java.io.IOException;
import java.net.Socket;

import common.SecretStore;
import common.protocol.ProtocolChannel;
import common.protocol.ProtocolRole;
import common.protocol.chap.CHAPProto;
import common.protocol.ticket.TicketProto;
import merrimackutil.util.NonceCache;

/**
 * Handles an new KDC connection.
 */
public class ConnectionHandler implements Runnable {

    private boolean doDebug = false;        // If we should to debug or not.
    private SecretStore secretStore;        // The secret store use by CHAP.
    private long validity;                  // How long a ticket should be valid for.
    private NonceCache cache;               // The nonce cache used for the KDC.
    private ProtocolChannel channel;        // The channel to communicate over.

    /**
     * Construct a new connection handler
     * 
     * @param sock the connection to work with.
     * @param doDebug {@code true} if the connection trace should be on.
     * @param secretStore a reference the secret storage.
     * @param validity the duration the ticket should be valid for (in milliseconds).
     * @param cache the nonce cache to use for the connection.
     * @throws IOException if the channel can not be read from or written to.
     * @throws IllegalArgumentException the socket is invalid.
     */
    public ConnectionHandler(Socket sock, boolean doDebug, SecretStore secretStore, long validity, NonceCache cache) throws IllegalArgumentException, IOException 
    {
        this.channel = new ProtocolChannel(sock);
        this.doDebug = doDebug;
        this.secretStore = secretStore;
        this.validity = validity;
        this.cache = cache;
    }

    /**
     * Handle the connection to the KDC.
     */
    @Override
    public void run() 
    {
        // If we should be doing debugging, turn it on for the
        // channel.
        if (doDebug)
            channel.toggleTracing();

        // Authenticate the user.
        CHAPProto chap = new CHAPProto(ProtocolRole.SERVER, cache);
        chap.initServer(secretStore);

        if (!chap.authenticate(channel)) {
            channel.closeChannel();
            return;
        }

        // Issue a ticket for the requested service.
        TicketProto tick = new TicketProto(ProtocolRole.SERVER);
        tick.initServer(secretStore, validity);
        tick.runTicketProto(channel);
        channel.closeChannel();
    }

}
