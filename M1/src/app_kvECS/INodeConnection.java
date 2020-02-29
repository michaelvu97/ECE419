package app_kvECS;

import shared.metadata.*;

public interface INodeConnection {
    /**
     * Sends a metadata set to a node.
     */
    public boolean sendMetadata(MetaDataSet mds) throws Exception;
}