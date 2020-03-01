package server;

import app_kvServer.IKVServer;
import shared.metadata.*;

public class MetaDataManager implements IMetaDataManager {
        
    private IKVServer _kvServer;
    private MetaDataSet _metaDataSet;
    private HashValue _currentServerHash;
    private MetaData _currentServerMetaData;

    public MetaDataManager(MetaDataSet mds, IKVServer kvServer) {
        _kvServer = kvServer;
        _metaDataSet = mds;

        _currentServerHash = HashUtil.ComputeHash(
            kvServer.getHostname(),
            kvServer.getPort()
        );

        if (mds == null)
            _currentServerMetaData = null;
        else
            _currentServerMetaData = mds.getServerForHash(_currentServerHash);
    }

    @Override
    public synchronized MetaDataSet getMetaData(){
        return _metaDataSet;
    }

    @Override
    public synchronized void updateMetaData(MetaDataSet mds) {
        if (mds == null)
            throw new IllegalArgumentException("mds is null");

        /**
         * 1. Determine the range of hashvalues that this server used to be
         *    responsible for
         * 2. Tell the serverStore to remove any entries that belong to that
         *    hash range.
         */

        MetaData newCurrentServerMetaData = mds.getServerForHash(
                _currentServerHash);

        if (newCurrentServerMetaData.compareTo(_currentServerMetaData) == 0) {
            // No changes to the current server's data, don't have to fix up
            // server's storage.
            _metaDataSet = mds;
            return;
        }

        _kvServer.requestLock();
        try {
            _kvServer.refocus(newCurrentServerMetaData.getHashRange());

            _currentServerMetaData = newCurrentServerMetaData;
            _metaDataSet = mds;
        } finally {
            _kvServer.requestUnlock();
        }
    }
}