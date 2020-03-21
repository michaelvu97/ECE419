package client;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
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
            entry.getValue().close();
        }

        _serverCommChannels.clear();
        _metaDataSet = null;

        if (ioe != null)
            throw ioe;
    }


    @Override
    public KVMessage sendRequest(KVMessage message) 
            throws Deserializer.DeserializationException, IOException {
        return sendRequest(message, 0);
    }

    @Override
    public KVMessage sendRequest(KVMessage message, int replicaNum)
            throws Deserializer.DeserializationException, IOException {
        if (message.getKey() == null)
            throw new IllegalArgumentException("Message contains a null key");
        if (replicaNum < 0 || 2 < replicaNum)
            throw new IllegalArgumentException("replicaNum out of range: " 
                    + replicaNum);

        // TODO TODO TODO: validate that if the request is for a replica, its
        // status type is allowed to be send (e.g. GET)

        HashValue hash = HashUtil.ComputeHashFromKey(message.getKey());
        logger.info("Hash of key is " + hash);

        int attempts = 0;
        int max_attempts = 3;

        while (attempts < max_attempts) {
            MetaData responsibleServer = _metaDataSet.getReplicaForHash(
                    hash, replicaNum);
            String targetName = responsibleServer.getName();
            logger.debug("sending to " + targetName);

            // Check our connection to the target server
            if (!_serverCommChannels.containsKey(targetName)) {
                // May throw an IOException.
                connectToServer(responsibleServer);
            } else if (!_serverCommChannels.get(targetName).isOpen()) {
                // Clean up this connection
                _serverCommChannels.get(targetName).close();
                _serverCommChannels.remove(targetName);
                connectToServer(responsibleServer);
            }

            ICommChannel responsibleCommChannel = 
                    _serverCommChannels.get(targetName);

            byte[] response = null;

            try {
                byte[] messageBytes = message.serialize();

                responsibleCommChannel.sendBytes(messageBytes);

                response = responsibleCommChannel.recvBytes();
            } catch (IOException e) {
                logger.warn("Comm failed, retrying", e);
                // Wait, and then try to refresh metadata
                try {
                    Thread.sleep(1000);
                } catch (Exception e_sleep){
                    // Swallow
                }
                tryRefreshMetadata();
                attempts++;
                continue;
            }

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
                  try {
                    Thread.sleep(1000);
                } catch (Exception e_sleep){
                    // Swallow
                }
                // Retry
            } else if (status == KVMessage.StatusType.SERVER_STOPPED) {
                _metaDataSet = MetaDataSet.Deserialize(
                    responseObj.getValueRaw()
                );
                try {
                    Thread.sleep(1000);
                } catch (Exception e_sleep){
                    // Swallow
                }
                logger.info("New Meta Data: " + _metaDataSet.toString());
            } else if (status == KVMessage.StatusType.SERVER_WRITE_LOCK) {
                // TODO
                // TODO
                // TODO
                // TODO
                try {
                    Thread.sleep(1000);
                } catch (Exception e_sleep){
                    // Swallow
                }
            } else {
                throw new IllegalStateException(
                    "Unknown server status: " + status
                );
            }
        }

        throw new IOException("Could not send message to server, attempted " 
                + attempts + " times");
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

    private void cleanupConnections() {
        Set<String> deadConnections = new HashSet<String>();
        for (Map.Entry<String, ICommChannel> kvp : _serverCommChannels.entrySet()) {
            if (!kvp.getValue().isOpen()) {
                deadConnections.add(kvp.getKey());
            }
        }

        for (String deadConn : deadConnections) {
            logger.debug("removing dead connection: " + deadConn);
            _serverCommChannels.remove(deadConn);
        }
    }

    /**
     * Attempts to find a valid metadata set from any of the server's it knows
     * about.
     */
    private void tryRefreshMetadata() {
        cleanupConnections();
        
        // In the event that a server fails, we should try to contact another
        // server to get an updated metadata set.
        for (MetaData m : _metaDataSet) {
            // First try to get metadata from a server that we have a valid
            // connection with.
            if (!_serverCommChannels.containsKey(m.getName()))
                continue;

            try {
                ICommChannel commChannel = _serverCommChannels
                        .get(m.getName());
                commChannel.sendBytes(
                        new KVMessageImpl(
                            KVMessage.StatusType.GET_METADATA,
                            null
                        ).serialize()
                );
                KVMessage res = KVMessageImpl.Deserialize(
                        commChannel.recvBytes());

                if (res.getStatus() == 
                        KVMessage.StatusType.GET_METADATA_SUCCESS) {

                    _metaDataSet = MetaDataSet.Deserialize(
                            res.getValueRaw());

                    logger.info("Metadata refreshed: " + 
                            _metaDataSet.toString());
                    return; // Metadata successfully refreshed
                }
            } catch (Exception e) {
                logger.warn("Could not refresh metadata", e);
            }
        }

        if (!_serverCommChannels.containsKey(_initialServerInfo.getName())) {
            ICommChannel commChannel = null;
            try {
                commChannel = new CommChannel(
                        new Socket(
                            _initialServerInfo.getHost(),
                            _initialServerInfo.getPort()
                        )
                );
                commChannel.sendBytes(new KVMessageImpl(
                        KVMessage.StatusType.GET_METADATA, null).serialize());

                KVMessage res = KVMessageImpl.Deserialize(
                        commChannel.recvBytes());

                if (res.getStatus() == 
                        KVMessage.StatusType.GET_METADATA_SUCCESS) {

                    _metaDataSet = MetaDataSet.Deserialize(
                            res.getValueRaw());

                    logger.info("Metadata refreshed" +
                            _metaDataSet.toString());
                    return; // Metadata successfully refreshed
                }
            } catch (Exception e) {
                logger.warn("Could not refresh metadata", e);
            } finally {
                if (commChannel != null)
                    commChannel.close();
            }
        }

        // Iterate through all known servers that don't have a connection
        // and try to get metadata
        for (MetaData m : _metaDataSet) {
            // First try to get metadata from a server that we have a valid
            // connection with.
            if (_serverCommChannels.containsKey(m.getName()))
                continue;

            ICommChannel commChannel = null;
            try {
                commChannel = new CommChannel(
                        new Socket(m.getHost(), m.getPort())
                );

                commChannel.sendBytes(
                        new KVMessageImpl(
                            KVMessage.StatusType.GET_METADATA,
                            null
                        ).serialize()
                );
                KVMessage res = KVMessageImpl.Deserialize(
                        commChannel.recvBytes());

                if (res.getStatus() == 
                        KVMessage.StatusType.GET_METADATA_SUCCESS) {

                    _metaDataSet = MetaDataSet.Deserialize(
                            res.getValueRaw());

                    logger.info("Metadata refreshed" + 
                            _metaDataSet.toString());
                    return; // Metadata successfully refreshed
                }
            } catch (Exception e) {
                logger.warn("Could not refresh metadata", e);
            } finally {
                if (commChannel != null)
                    commChannel.close();
            }
        }
    }
}