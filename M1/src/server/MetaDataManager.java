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
         * 1. Determine the range of hashvalues that this server used to be
         *    responsible for
         * 2. Tell the serverStore to remove any entries that belong to that
         *    hash range.
         */

        MetaData newCurrentServerMetaData = mds.getServerForHash(
                _currentServerHash);
        if(_currentServerMetaData != null)
            logger.debug("new metadata = " + _currentServerMetaData.toString() + " old metadata = " + newCurrentServerMetaData.toString());
        if (_currentServerMetaData != null 
                && newCurrentServerMetaData.equalTo(_currentServerMetaData) == 0) {
            // No changes to the current server's data, don't have to fix up
            // server's storage.
            _metaDataSet = mds;
            logger.debug("1. _currentServerMetaData is " + _currentServerMetaData.toString());
            return;
        }

        _kvServer.requestLock();
        try {
            _kvServer.refocus(newCurrentServerMetaData.getHashRange());

            _currentServerMetaData = newCurrentServerMetaData;
            _metaDataSet = mds;
            logger.debug("2. _currentServerMetaData is " + _currentServerMetaData.toString());
        } finally {
            _kvServer.requestUnlock();
        }
    }
}