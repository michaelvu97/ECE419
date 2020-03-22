package app_kvECS;

import shared.metadata.*;
import java.util.List;
import shared.messages.KVAdminMessage;

public interface INodeConnection {

    /**
     * Sends a list of nodes to kill to a node.
     */
    public void sendKillMessage() throws Exception;

    public boolean sendCloseMessage();

    /**
     * Tell a server tp delete its data
     */
    public void sendDeleteData() throws Exception;

    /**
     * Sends a metadata set to a node.
     */
    public void sendMetadata(MetaDataSet mds) throws Exception;

    /** 
     * Sends a transfer request to a node.
     */
    public KVAdminMessage sendTransferRequest(TransferRequest tr) throws Exception;
    
    /**
     * Returns the name of this node
     */
    public String getNodeName();
}
