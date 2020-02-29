package app_kvECS;

import shared.metadata.*;

public interface INodeConnection {
    /**
     * Sends a metadata set to a node.
     */
    public void sendMetadata(MetaDataSet mds) throws Exception;

    /** 
     * Sends a transfer request to a node.
     */
    public void sendTransferRequest(TransferRequest tr) throws Exception;
    
    /**
     * Returns the name of this node
     */
    public String getNodeName();
}