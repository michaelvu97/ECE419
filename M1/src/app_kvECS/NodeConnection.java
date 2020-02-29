package app_kvECS;

import java.io.IOException;
import java.net.Socket;

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

    public NodeConnection(Socket socket, Acceptor acceptor) {
        super(socket, acceptor);

        // TODO
    }

    @Override
    public void run() {
        throw new IllegalStateException(
                "Cannot run a NodeConnection, disallowed");
    }


    @Override
    public void work() throws Exception {
        throw new IllegalStateException("Cannot call work on NodeConnection");
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
    public void sendTransferRequest(TransferRequest tr) throws Exception {
        try {
            KVAdminMessage messageToSend = new KVAdminMessage(
                    KVAdminMessage.StatusType.TRANSFER_REQUEST,
                    tr.serialize()
            );
            
            this.commChannel.sendBytes(messageToSend.serialize());
             byte[] responseBytes = this.commChannel.recvBytes();
            KVAdminMessage response = KVAdminMessage.Deserialize(responseBytes);
            if (response.getStatus() != KVAdminMessage.StatusType
                        .UPDATE_METADATA_REQUEST_SUCCESS) {
                _logger.warn("Send transfer request failed on node");
                throw new Exception("Send transfer request failed on node");
            }
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