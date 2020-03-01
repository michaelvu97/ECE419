package server;

import app_kvServer.IKVServer;
import shared.metadata.*;
import org.apache.log4j.*;

public class MetaDataManager implements IMetaDataManager {
    private static Logger logger = Logger.getRootLogger();
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
    public synchronized boolean isInRange(HashValue value) {
        if (value == null)
            throw new IllegalArgumentException("value");

        if (_currentServerMetaData == null)
            return false;
        logger.debug("_currentServerMetaData is " + _currentServerMetaData.toString());
        return _currentServerMetaData.getHashRange().isInRange(value);
    }

    @Override
    public synchronized void updateMetaData(MetaDataSet mds) {
        if (mds == null)
            throw new IllegalArgumentException("mds is null");

        /**
         * Case 1: This is the first metadata update
         * Case 2: We have grown
         * Case 3: We have shrunk
         * Case 4: Nothing is different for us.
         */
        
        MetaData newCurrentServerMetaData = 
                mds.getServerForHash(_currentServerHash);

        
        if (!newCurrentServerMetaData.getName().equals(_kvServer.getName())) {
            newCurrentServerMetaData = null;
        }

        if (_currentServerMetaData != null 
                && newCurrentServerMetaData != null
                && newCurrentServerMetaData.equals(_currentServerMetaData)) {
            // No changes to the current server's data, don't have to fix up
            // server's storage.
            _metaDataSet = mds;
            return;
        }

        // _kvServer.refocus(newCurrentServerMetaData.getHashRange());
        _currentServerMetaData = newCurrentServerMetaData;
        _metaDataSet = mds;
    }
}