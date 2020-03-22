package app_kvECS;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.Socket;
import java.io.IOException;

import org.apache.log4j.Logger;

import shared.messages.KVAdminMessage;
import shared.metadata.*;
import shared.network.*;
import shared.serialization.*;

/**
 * Note that node connection is no longer a runnable, it is synchronous.
 * It is only used to send requests to nodes.
 */
public final class NodeConnection extends Connection implements INodeConnection {

    private static Logger _logger = Logger.getRootLogger();

    private String _name;

    public NodeConnection(Socket socket, Acceptor acceptor, String name) {
        super(socket, acceptor);
        
       if (name == null || name.length() == 0)
          throw new IllegalArgumentException("name is null or empty"); 

        _name = name;
    }

    @Override
    public void run() {
        throw new IllegalStateException(
                "Cannot run a NodeConnection, disallowed");
    }

    @Override
    public void stop() {
        _logger.info("Stopping node connection thread.");

        this.isOpen = false;
        parentAcceptor.alertClose(this);

        try {
            this.socket.close();
        } catch (IOException ioe) {
            _logger.warn("Closing node connection", ioe);
        }
    }



    @Override
    public void work() throws Exception {
        throw new IllegalStateException("Cannot call work on NodeConnection");
    }

    @Override
    public String getNodeName() {
        return _name;
    }

    @Override
    public void sendKillMessage() throws Exception {
        try {
            KVAdminMessage messageToSend = new KVAdminMessage(
                    KVAdminMessage.StatusType.KYS,
                    null
            );

            this.commChannel.sendBytes(messageToSend.serialize());
        } catch (IOException ioe) {
            _logger.error("Send kill message failed I/O", ioe);
            throw ioe;
        }
    }

    @Override
    public boolean sendCloseMessage() {
        try {
            KVAdminMessage messageToSend = new KVAdminMessage(
                    KVAdminMessage.StatusType.CYS,
                    null    
            );
            this.commChannel.sendBytes(messageToSend.serialize());
        } catch (IOException ioe) {
            _logger.error("Send close message failed I/O", ioe);
            return false;
        }

        try {
            this.commChannel.recvBytes();
        } catch (IOException ioe) {
            _logger.info("Target closed");
        }
        return true;
    }

    @Override
    public void sendMetadata(MetaDataSet mds) throws Exception {
        if (mds == null)
            throw new IllegalArgumentException("mds is null");

        try {
            KVAdminMessage messageToSend = new KVAdminMessage(
                    KVAdminMessage.StatusType.UPDATE_METADATA_REQUEST,
                    mds.serialize()
            );

            this.commChannel.sendBytes(messageToSend.serialize());
            byte[] responseBytes = this.commChannel.recvBytes();
            KVAdminMessage response = KVAdminMessage.Deserialize(responseBytes);
            if (response.getStatus() != KVAdminMessage.StatusType
                        .UPDATE_METADATA_REQUEST_SUCCESS) {
                _logger.warn("Send metadata failed on node");
                throw new Exception("Send metadata failed on node");
            }
        } catch (IOException ioe) {
            _logger.error("Send metadata failed I/O", ioe);
            throw ioe;
        } catch (Deserializer.DeserializationException dse) {
            _logger.error("Send metadata failed, invalid node response", dse);
            throw dse;
        }
    }

     @Override
    public void sendDeleteData() throws Exception {
        try {
            KVAdminMessage messageToSend = new KVAdminMessage(
                    KVAdminMessage.StatusType.DELETE_DATA,
                    null
            );

            this.commChannel.sendBytes(messageToSend.serialize());
            byte[] responseBytes = this.commChannel.recvBytes();
            KVAdminMessage response = KVAdminMessage.Deserialize(responseBytes);
            if (response.getStatus() != KVAdminMessage.StatusType
                        .DELETE_SUCCESS) {
                _logger.warn("Send delete data failed on node");
                throw new Exception("Send delete data failed on node");
            }
        } catch (IOException ioe) {
            _logger.error("Send delete data failed I/O", ioe);
            throw ioe;
        } catch (Deserializer.DeserializationException dse) {
            _logger.error("Send delete data failed, invalid node response", dse);
            throw dse;
        }
    }

    @Override
    public KVAdminMessage sendTransferRequest(TransferRequest tr) throws Exception {
        try {
            KVAdminMessage messageToSend = new KVAdminMessage(
                    KVAdminMessage.StatusType.TRANSFER_REQUEST,
                    tr.serialize()
            );
            
            this.commChannel.sendBytes(messageToSend.serialize());
             byte[] responseBytes = this.commChannel.recvBytes();
            KVAdminMessage response = KVAdminMessage.Deserialize(responseBytes);

            if (response.getStatus() != KVAdminMessage.StatusType
                        .TRANSFER_REQUEST_SUCCESS) {
                _logger.warn("Send transfer request failed on node");
                throw new Exception("Send transfer request failed on node");
            }

            _logger.info("Transfer request response: " + response.getStatus());

            // return KVAdminMessage containing transfer success or failure.
            return response;

        } catch (IOException ioe) {
            _logger.error("Send transfer request failed I/O", ioe);
            throw ioe;
        } catch (Deserializer.DeserializationException dse) {
            _logger.error("Send transfer request failed, invalid node response",
                    dse);
            throw dse;
        }
    }
}
