package ecs;

import shared.metadata.*;

public class ECSNode implements IECSNode {

    private String _nodeHost = null;
    private int _nodePort = 0;
    private String _nodeName = null;
    private ECSNodeFlag _flag = null;
    private MetaData _metaData = null;
    private String[] _nodeHashRange = null;
    private String _cacheStrategy = null;
    private int _cacheSize = 0;
    private ServerInfo _serverInfo = null;
    private String _nodeHash = null;

    // TODO:
    // - flag
    // - node hash range

    public ECSNode(String nodeHost, String nodeName, int nodePort, String cacheStrategy, 
        int cacheSize) 
    {
        _nodePort = nodePort; 
        _nodeName = nodeName;
        _cacheSize = cacheSize;
        _nodeHost = nodeHost;
        _cacheStrategy = cacheStrategy;

        _serverInfo = new ServerInfo(_nodeName, _nodeHost, _nodePort);
        _nodeHash = _serverInfo.getHash();
        _metaData = new MetaData(_nodeName, _nodeHost, _nodePort, _nodeHash, _nodeHash); 
    }

    public String getNodeName() {
        return _nodeName;
    }

    public String getNodeHost() {
        return _nodeHost;
    }

    public int getNodePort() {
        return _nodePort;
    }

    public String[] getNodeHashRange(){
        return _nodeHashRange;
    }

    public void setFlag(ECSNodeFlag flag){

    }

    public ECSNodeFlag getFlag() {
        return _flag;
    }

    public MetaData getMetaData() {
        return _metaData;
    }
}
