package client;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import shared.comms.*;
import shared.messages.*;
import shared.metadata.*;
import shared.serialization.*;

public final class ServerCommManager implements IServerCommManager {

    private Logger logger = Logger.getRootLogger();

    // Null until an intial response from a server.
    private MetaDataSet _metaDataSet = null;

    // Dumb-ish implementation, save the server sockets in a hashmap.
    // Maps from server node name -> socket.
    private HashMap<String, ICommChannel> _serverCommChannels = 
            new HashMap<String, ICommChannel>();

    private ServerInfo _initialServerInfo;

    public ServerCommManager(ServerInfo initialServerInfo) {
        if (initialServerInfo == null)
            throw new IllegalArgumentException("initialServerInfo is null");

        _initialServerInfo = initialServerInfo;
    }

    @Override
    public void connect() throws IOException {
        logger.debug("Connecting to initial server");

        Socket clientSocket = new Socket(
            _initialServerInfo.getHost(),
            _initialServerInfo.getPort()
        );

        try {

            ICommChannel commChannel = new CommChannel(clientSocket);

            // Get the initial metadata
            commChannel.sendBytes(
                    new KVMessageImpl(
                            KVMessage.StatusType.GET_METADATA,
                            null,
                            (byte[]) null
                    )
                    .serialize()
            );

            KVMessage response = KVMessageImpl.Deserialize(commChannel.recvBytes());

            if (response.getStatus() == KVMessage.StatusType.GET_METADATA_SUCCESS) {
                _metaDataSet = MetaDataSet.Deserialize(response.getValueRaw());
                logger.debug("Received initial metadata set: " + _metaDataSet);
            } else {
                throw new IOException(
                        "Invalid response from server: " + response.getStatus());
            }
        } catch (Deserializer.DeserializationException dse) {
            throw new IOException(dse);
        } finally {
            clientSocket.close();
        }

        logger.info("connection established");
    }

    @Override
    public synchronized void disconnect() throws IOException {

        IOException ioe = null;

        // Close all sockets
        for (Map.Entry<String, ICommChannel> entry : 
                _serverCommChannels.entrySet()) {
            try {
                entry.getValue().getSocket().close();
            } catch (IOException e) {
                ioe = e;
                logger.error("Could not disconnect from server: " 
                        + entry.getKey());
            }
        }

        _serverCommChannels.clear();
        _metaDataSet = null;

        if (ioe != null)
            throw ioe;
    }

    @Override
    public KVMessage sendRequest(KVMessage message) 
            throws Deserializer.DeserializationException, IOException {
        if (message.getKey() == null)
            throw new IllegalArgumentException("Message contains a null key");

        HashValue hash = HashUtil.ComputeHashFromKey(message.getKey());
        logger.info("Hash of key is " + hash);

        while (true) {
            MetaData responsibleServer = _metaDataSet.getServerForHash(hash);
            logger.debug("sending to " + responsibleServer.getName());

            if (!_serverCommChannels.containsKey(responsibleServer.getName())) {
                // May throw an IOException.
                connectToServer(responsibleServer);
            }

            ICommChannel responsibleCommChannel = 
                    _serverCommChannels.get(responsibleServer.getName());

            byte[] messageBytes = message.serialize();

            responsibleCommChannel.sendBytes(messageBytes);

            // Should probably trycatch?
            byte[] response = responsibleCommChannel.recvBytes();

            KVMessage responseObj = KVMessageImpl.Deserialize(response);

            logger.info("Received server response: " + responseObj.toString());

            KVMessage.StatusType status = responseObj.getStatus();

            if (isStatusSuitableForClient(status)) {
                return responseObj;
            }

            // The response was server-issue related.
            if (status == KVMessage.StatusType.SERVER_NOT_RESPONSIBLE) {
                // Need to update metadata and try again.
                _metaDataSet = MetaDataSet.Deserialize(
                    responseObj.getValueRaw()
                );
                logger.info("New Meta Data: " + _metaDataSet.toString());

                // Retry
            } else if (status == KVMessage.StatusType.SERVER_STOPPED) {
                  _metaDataSet = MetaDataSet.Deserialize(
                    responseObj.getValueRaw()
                );
                logger.info("New Meta Data: " + _metaDataSet.toString());
            } else if (status == KVMessage.StatusType.SERVER_WRITE_LOCK) {
                // TODO
                // TODO
                // TODO
                // TODO
            } else {
                throw new IllegalStateException(
                    "Unknown server status: " + status
                );
            }
        }
    }

    private static boolean isStatusSuitableForClient(
                KVMessage.StatusType status) {
        return status != KVMessage.StatusType.SERVER_NOT_RESPONSIBLE
                && status != KVMessage.StatusType.SERVER_STOPPED
                && status != KVMessage.StatusType.SERVER_WRITE_LOCK;
    }

    private void connectToServer(MetaData serverMetaData) 
            throws IOException {

        String name = serverMetaData.getName();
        String host = serverMetaData.getHost();
        int port = serverMetaData.getPort();

        // Check if we're already connected to this server
        if (_serverCommChannels.containsKey(name)) {
            return;
        }

        logger.debug("Connecting to " + name);
        Socket clientSocket = new Socket(
            host,
            port
        );

        ICommChannel commChannel = new CommChannel(clientSocket);

        _serverCommChannels.put(name, commChannel);

        logger.info("connection established");
    }
}