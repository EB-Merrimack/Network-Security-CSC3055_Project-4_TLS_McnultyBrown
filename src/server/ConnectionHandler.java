
package server;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.Socket;
import java.util.Base64;

import common.SecretStore;
import common.protocol.Message;
import common.protocol.ProtocolChannel;
import common.protocol.messages.PostBuilder;
import common.protocol.messages.PostMessage;
import merrimackutil.util.NonceCache;

public class ConnectionHandler implements Runnable {

    private ProtocolChannel channel;
    private NonceCache nonceCache;
    private boolean doDebug = false;
    private String serviceName;
    private String secret;
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
     * Handles the Bulitin service connection.
     */
    @Override
    public void run() {

        runCommunication();
        channel.closeChannel();
      }

      /**
       * Run the communication between the service and the client after the handshake.
       */
      private void runCommunication() {

          PostMessage msg = null;
          String payload = null;

          try {
              msg = (PostMessage) channel.receiveMessage();

           

        
              payload = msg.getDecryptedPayload(sessionKey);

              if (payload == null)
                  return;
          } catch (InvalidObjectException ex) {
              System.out.println(ex);
              return;
          }
          channel.sendMessage((Message) PostBuilder.buildMessage(payload));
      }
    
}
