package app_kvServer;

import java.io.IOException;
import java.net.Socket;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import logger.LogSetup;

import app_kvServer.IKVServer;
import shared.comms.*;
import shared.messages.KVAdminMessage;
import shared.metadata.*;
import shared.serialization.*;
import server.*;

public final class ECSCommandReceiver implements IECSCommandReceiver {

    private static Logger logger = Logger.getRootLogger();
    private IKVServer _kvServer;
    private IMetaDataManager _metaDataManager;

    private String _ecsLoc;
    private int _ecsPort;
    private ICommChannel _commChannel;

    private boolean _running = false;

    /**
     * Connects on construction.
     */
    public ECSCommandReceiver(IKVServer kvServer, 
            IMetaDataManager metaDataManager,
            String ecsLoc,
            int ecsPort) throws IOException {
        if (kvServer == null)
            throw new IllegalArgumentException("kvServer is null");
        if (metaDataManager == null)
            throw new IllegalArgumentException("metaDataManager is null");

        _kvServer = kvServer;
        _metaDataManager = metaDataManager;

        _ecsLoc = ecsLoc;
        _ecsPort = ecsPort;

        connect();
    }

    public void connect() throws IOException {
        logger.debug("ECS Command Receiver connecting to " + _ecsLoc + ":" 
                + _ecsPort);
        Socket clientSocket = new Socket(
            _ecsLoc,
            _ecsPort
        );

        _commChannel = new CommChannel(clientSocket);
        _commChannel.sendBytes(_kvServer.getName().getBytes());
        logger.info("Connected to ECS");
    }

    @Override
    public void run() {
        logger.info("ECS Command Receiver running");
        _running = true;
        while(_running) {
            // Listen for commands
            try {
                byte[] recvBytes = _commChannel.recvBytes();
                KVAdminMessage message = KVAdminMessage.Deserialize(recvBytes);
                KVAdminMessage result = handleCommand(message);
                _commChannel.sendBytes(result.serialize());
            } catch (Exception e) {
                logger.error(e);
                _running = false;
            }
        }

        logger.info("ECS Command Receiver stopped");
    }

    private KVAdminMessage handleCommand(KVAdminMessage request) 
            throws Exception {
        switch (request.getStatus()) {
            case UPDATE_METADATA_REQUEST:
                return onUpdateMetadataRequest(request);
            case TRANSFER_REQUEST:
                return onTransferRequest(request);
            default:
                throw new Exception("Invalid command from ECS: " + request.getStatus());
        }
    }

    /**
     * Called when a transfer request is receieved.
     */
    public KVAdminMessage onTransferRequest(KVAdminMessage transferRequest) {
        // TODO
        return null;
    }

    /**
     * Called when an update metadata request is received.
     */
    public KVAdminMessage onUpdateMetadataRequest(KVAdminMessage updateMetadataRequest) {
        
        // Deserialize the metadata
        MetaDataSet mds;

        try {
            mds = MetaDataSet.Deserialize(updateMetadataRequest.getPayload());
        } catch (Deserializer.DeserializationException dse) {
            logger.error("Received invalid metadata from server", dse);
            return new KVAdminMessage(
                    KVAdminMessage.StatusType.UPDATE_METADATA_REQUEST_FAILURE,
                    "Could not parse metadata payload".getBytes()
            );
        }

        _metaDataManager.updateMetaData(mds);
        return new KVAdminMessage(KVAdminMessage.StatusType.UPDATE_METADATA_REQUEST_SUCCESS, null);
    }


}
