package server;

import shared.metadata.*;

/**
 * Threadsafe class for managing the server's metadata.
 */
public interface IMetaDataManager {
    /**
     * @return the current set of metadata.
     * // TODO: what happens if no metadata is available?
     */
    public MetaDataSet getMetaData();

    public void updateMetaData(MetaDataSet mds);
}