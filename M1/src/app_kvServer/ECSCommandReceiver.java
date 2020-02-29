package app_kvServer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import logger.LogSetup;

import app_kvServer.IKVServer;
import shared.messages.KVAdminMessage;
import shared.metadata.*;
import shared.serialization.*;
import server.*;

public final class ECSCommandReceiver implements IECSCommandReceiver {

    private static Logger logger = Logger.getRootLogger();
    private IKVServer _kvServer;
    private IMetaDataManager _metaDataManager;

    /**
     * Connects on construction.
     */
    public ECSCommandReceiver(IKVServer kvServer, 
            IMetaDataManager metaDataManager,
            String ECSLoc,
            int ECSPort) {
        if (kvServer == null)
            throw new IllegalArgumentException("kvServer is null");
        if (metaDataManager == null)
            throw new IllegalArgumentException("metaDataManager is null");

        _kvServer = kvServer;
        _metaDataManager = metaDataManager;

        connect();
    }

    public void connect() {
        // TODO
    }

    /**
     * Called when a transfer request is receieved.
     */
    public void onTransferRequest(KVAdminMessage transferRequest) {
        // TODO
    }

    /**
     * Called when an update metadata request is received.
     */
    public void onUpdateMetadataRequest(KVAdminMessage updateMetadataRequest) {
        
        // Deserialize the metadata
        MetaDataSet mds;

        try {
            mds = MetaDataSet.Deserialize(updateMetadataRequest.getPayload());
        } catch (Deserializer.DeserializationException dse) {
            logger.error("Received invalid metadata from server", dse);
            return;
        }

        _metaDataManager.updateMetaData(mds);
    }


}