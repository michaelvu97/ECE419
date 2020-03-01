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
        connectToServer(_initialServerInfo);
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
        if (_metaDataSet == null) {
            // TODO: acquire initial metadata?
        }
        if (message.getKey() == null)
            throw new IllegalArgumentException("Message contains a null key");

        HashValue hash = HashUtil.ComputeHashFromKey(message.getKey());

        while (true) {
            MetaData responsibleServer = _metaDataSet.getServerForHash(hash);

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

                // Retry
            } else if (status == KVMessage.StatusType.SERVER_STOPPED) {
                // TODO
                // TODO
                // TODO
                // TODO
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
        connectToServer(new ServerInfo(
            serverMetaData.getName(),
            serverMetaData.getHost(),
            serverMetaData.getPort())
        );
    }

    private void connectToServer(ServerInfo serverInfo) 
            throws IOException {
        Socket clientSocket = new Socket(
            serverInfo.getHost(),
            serverInfo.getPort()
        );

        ICommChannel commChannel = new CommChannel(clientSocket);
        _serverCommChannels.put(serverInfo.getName(), commChannel);

        // Make a dummy metadata set
        ArrayList<ServerInfo> fakeServerInfo = new ArrayList<ServerInfo>();
        fakeServerInfo.add(_initialServerInfo);
        _metaDataSet = MetaDataSet.CreateFromServerInfo(fakeServerInfo);

        logger.info("connection established");
    }
}