package app_kvServer;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import logger.LogSetup;

import app_kvECS.*;
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
                logger.error("Lost comms to ECS", e);
                _running = false;

                // Stop the kv server (gracefully). We don't want the server to run off on its own
                // without an ECS.
                _kvServer.close();
                _kvServer.kill(); // ? Maybe ?
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
            case KYS:
                onKillNodeRequest();
            default:
                throw new Exception("Invalid command from ECS: " + request.getStatus());
        }
    }

    /**
     * Called when a kill node request is receieved.
     */
    public void onKillNodeRequest() {
        _kvServer.kill(); // bye bye 
    }

    /**
     * Called when a transfer request is receieved.
     */
    public KVAdminMessage onTransferRequest(KVAdminMessage transferRequest) {
        // TODO
        //parse message
        TransferRequest tr = null;
        try {
            tr = TransferRequest.Deserialize(transferRequest.getPayload());
        } catch (Deserializer.DeserializationException dse) {
            logger.error("Received invalid transfer request from server", dse);
            return new KVAdminMessage(
                    KVAdminMessage.StatusType.TRANSFER_REQUEST_FAILURE,
                    "Could not parse transfer request payload".getBytes()
            );
        }

        // Confirm that we are the source
        String fromServ = tr.getFromName();
        if (!fromServ.equals(_kvServer.getName())) {
            logger.warn("Received transfer request, not responsible.");
            return new KVAdminMessage(
                    KVAdminMessage.StatusType.TRANSFER_REQUEST_FAILURE,
                    "This is not the right server for transfer request".getBytes()
            );
        }
        boolean success = false;
        String toServ = tr.getToName();
        MetaDataSet newMetaDataSet = tr.getNewMetaDataset();

        MetaData targetServ = newMetaDataSet.getMetaDataByName(toServ);

        _kvServer.writeLock();
        
        success = _kvServer.transferDataToServer(targetServ);

        if (success) {
            _metaDataManager.updateMetaData(newMetaDataSet);
        }
        _kvServer.writeUnlock();

        // create new KVAdminMessage based on transfer success or failure.
        return new KVAdminMessage(
                (success ? KVAdminMessage.StatusType.TRANSFER_REQUEST_SUCCESS 
                : KVAdminMessage.StatusType.TRANSFER_REQUEST_FAILURE),
                "Done".getBytes()
        );
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

        logger.debug("Received new metadata: " + mds.toString());
    
        MetaData[] oldReplicas = _metaDataManager.getReplicas();        
        _metaDataManager.updateMetaData(mds);
        MetaData[] newReplicas = _metaDataManager.getReplicas();

        MetaData replica1 = newReplicas[0];
        MetaData replica2 = newReplicas[1];
        MetaData oldRep1 = oldReplicas[0];
        MetaData oldRep2 = oldReplicas[1];

        logger.info("checking if a transfer is needed for backups");
        if(replica1!=null){
            if(!((oldRep1!=null && replica1.getName() == oldRep1.getName()) || (oldRep2!=null && replica1.getName() == oldRep2.getName()))){
                logger.info("tranfering my data to replica 1");
                _kvServer.transferDataToNewReplica(replica1);  
            }
        }
        if(replica2!=null){
            if(!((oldRep1!=null && replica1.getName() == oldRep1.getName()) || (oldRep2!=null && replica1.getName() == oldRep2.getName()))){
                logger.info("tranfering my data to replica 2");
                _kvServer.transferDataToNewReplica(replica2);
            }
        }

        logger.info("New metaData is: " + _metaDataManager.getMetaData());
        return new KVAdminMessage(KVAdminMessage.StatusType.UPDATE_METADATA_REQUEST_SUCCESS, null);
    }


}
