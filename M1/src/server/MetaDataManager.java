package server;

import app_kvServer.IKVServer;
import shared.metadata.*;
import org.apache.log4j.*;

public class MetaDataManager implements IMetaDataManager {
    private static Logger logger = Logger.getRootLogger();
    private IKVServer _kvServer;
    private MetaDataSet _metaDataSet = null;
    private HashValue _currentServerHash;
    private MetaData _primaryMetaData = null;
    private MetaData[] _replicaMetadata = new MetaData[2];

    public MetaDataManager(MetaDataSet mds, IKVServer kvServer) {
        _kvServer = kvServer;

        _currentServerHash = HashUtil.ComputeHash(
            kvServer.getHostname(),
            kvServer.getPort()
        );

        if (mds != null)
            updateMetaDataSet(mds);
    }

    private void updateMetaDataSet(MetaDataSet newMds) {
        if (newMds == null)
            throw new IllegalArgumentException("newMds is null");
        _metaDataSet = newMds;

        _primaryMetaData = newMds.getPrimaryForHash(_currentServerHash);
        _replicaMetadata[0] = newMds.getReplicaForHash(_currentServerHash, 1);
        _replicaMetadata[1] = newMds.getReplicaForHash(_currentServerHash, 2);

        if (_replicaMetadata[0].getHashRange().equals(
                    _primaryMetaData.getHashRange()))
            _replicaMetadata[0] = null;

        if (_replicaMetadata[1].getHashRange().equals(
                    _primaryMetaData.getHashRange()))
            _replicaMetadata[1] = null;
    }

    @Override
    public synchronized MetaDataSet getMetaData(){
        return _metaDataSet;
    }

    @Override
    public synchronized boolean isInRange(HashValue value) {
        if (value == null)
            throw new IllegalArgumentException("value");

        if (_primaryMetaData == null)
            return false;
        logger.debug("_primaryMetaData is " + _primaryMetaData.toString());
        return _primaryMetaData.getHashRange().isInRange(value);
    }

    @Override
    public synchronized boolean isInReplicaRange(HashValue value) {
        if (value == null)
            throw new IllegalArgumentException("value is null");

        if (_primaryMetaData == null)
            return false;

        return _metaDataSet.isInReplicaRange(value, _primaryMetaData);
    }

    @Override
    public synchronized MetaData getMyMetaData() {
        return _primaryMetaData;
    }

    @Override
    public synchronized MetaData[] getReplicas() {
        return _replicaMetadata;
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
                mds.getPrimaryForHash(_currentServerHash);

        
        if (!newCurrentServerMetaData.getName().equals(_kvServer.getName())) {
            newCurrentServerMetaData = null;
        }

        if (_primaryMetaData != null 
                && newCurrentServerMetaData != null
                && newCurrentServerMetaData.equals(_primaryMetaData)) {
            // No changes to the current server's data, don't have to fix up
            // server's storage.
            updateMetaDataSet(mds);
            return;
        }
        
        // Disabled because it messes up data transfer
        // if(newCurrentServerMetaData != null){
        //     _kvServer.refocus(newCurrentServerMetaData.getHashRange());
        // } else {
        //     _kvServer.clearStorage();
        // }

        updateMetaDataSet(mds);
        
        if (_primaryMetaData == null)
            _kvServer.setServerState(IKVServer.ServerStateType.STOPPED);
        else
            _kvServer.setServerState(IKVServer.ServerStateType.STARTED);
    }
}