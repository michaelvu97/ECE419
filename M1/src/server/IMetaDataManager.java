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

    public MetaData getMyMetaData();

    // Returns all replicas for the current server
    public MetaData[] getReplicas();

    /**
     * Atomically updates the metadata set.
     * Also removes any KV entries from KVServer that are no longer stored on
     * this server.
     */
    public void updateMetaData(MetaDataSet mds);

    public boolean isInRange(HashValue value);
}