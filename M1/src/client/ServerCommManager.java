package client;

import java.util.HashMap;
import java.io.IOException;

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

    public ServerCommManager() {
        
        // We need to figure out an intial server to connect to in order to 
        // populate our metadata set.

        // TODO
        // TODO
        // TODO
        // TODO
    }

    @Override
    public KVMessage sendRequest(KVMessage message) 
            throws Deserializer.DeserializationException, IOException {
        if (_metaDataSet == null) {
            // TODO: acquire initial metadata?
        }
        if (message.getKey() == null)
            throw new IllegalArgumentException("Message contains a null key");

        HashValue hash = HashValue.CreateFromHashString(message.getKey());

        while (true) {
            MetaData responsibleServer = _metaDataSet.getServerForHash(hash);

            if (!_serverCommChannels.containsKey(responsibleServer.getName())) {
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

    private void connectToServer(MetaData serverMetaData) {
        ICommChannel commChannel = null;

        // TODO
        // TODO
        // TODO

        _serverCommChannels.put(serverMetaData.getName(), commChannel);
    }
}