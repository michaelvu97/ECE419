package server;

import shared.metadata.*;

public class MetaDataManager implements IMetaDataManager {
    
    private MetaDataSet _metaDataSet = null;

    @Override
    public synchronized MetaDataSet getMetaData(){
        return _metaDataSet;
    }
}