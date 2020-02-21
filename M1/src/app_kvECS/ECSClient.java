package app_kvECS;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import logger.LogSetup;

import org.apache.zookeeper.*;

import ecs.*;
import shared.metadata.*;

public class ECSClient implements IECSClient {

    // TODO: update all servers' meta data every time a new server is added.

    private String _configFilePath;
    private ZooKeeper _zoo = null;
    private static Logger logger = Logger.getRootLogger();
    private List<ServerInfo> allServerInfo = new ArrayList<ServerInfo>();
    private Map<String, IECSNode> allNodes = new HashMap<String, IECSNode>();

    public ECSClient(String configFilePath) {
        if (configFilePath == null || configFilePath.length() == 0)
            throw new IllegalArgumentException("configFilePath");

        _configFilePath = configFilePath;

        // TODO: extract all servers in config into allServerInfo.
    }

    @Override
    public boolean start() {
        
        // unsure about this
        try {
            _zoo = new ZooKeeper("localhost:2181", 10000, new ECSWatcher());
            // data monitor?
        } catch (IOException e){
            _zoo = null;
        }

        // TODO
        return false;
    }

    @Override
    public boolean stop() {
        // TODO
        return false;
    }

    @Override
    public boolean shutdown() {
        // TODO
        return false;
    }

    @Override
    public ServerInfo getNextAvailableServer() {
       
        for(int i = 0; i < allServerInfo.size(); i++) {
            if (allServerInfo.get(i).getAvailability()) {
                return allServerInfo.get(i);
            }
        }
        return null;
    }

    @Override
    public IECSNode addNode(String cacheStrategy, int cacheSize) {
        
        ECSNode newNode = null;
        ServerInfo newServer = getNextAvailableServer();
        
        if (newServer != null) {
            newNode = new ECSNode(newServer.getHost(), newServer.getName(), 
               newServer.getPort(), cacheStrategy, cacheSize); 

            // TODO: add the new node to map allNodes.

            // ssh call to start KV server
            // TODO: write the actual kc_server.sh scrip.
            Process proc = null;
            String script = "kv_server.sh";

            Runtime run = Runtime.getRuntime();
            String cmd[] = {script /*AGUMENTS FOR SCRIPT HERE */};

            try {
                proc = run.exec(cmd);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            // TODO: CREATE & SET METADATA FOR ALL THE SERVERS HERE!
            // use CreateFromServerInfo(Collection<ServerInfo> serverInfos) from MetaDataSet class.

            return newNode;

        } else {
            // TODO no servers left! do something about it.
        }
        return null;
    }

    @Override
    public Collection<IECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        // TODO: call addNode count # of times
        return null;
    }

    @Override
    public Collection<IECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        // TODO
        return null;
    }

    @Override
    public boolean awaitNodes(int count, int timeout) throws Exception {
        // TODO
        return false;
    }

    @Override
    public boolean removeNodes(Collection<String> nodeNames) {
        // TODO
        return false;
    }

    @Override
    public Map<String, IECSNode> getNodes() {
        // TODO ???
        return allNodes;
    }

    @Override
    public IECSNode getNodeByName(String name) {
        // TODO
        return null;
    }

    public static void main(String[] args) {
        // Start Admin CLI
        ECSClient client = new ECSClient("ecs.config");
        client.start();
    }
}
