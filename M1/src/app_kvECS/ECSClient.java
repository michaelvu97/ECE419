package app_kvECS;

import ecs.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.io.IOException;
import java.lang.*;

import shared.metadata.*;
import org.apache.zookeeper.*;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ECSClient implements IECSClient {

    // TODO: update all servers' meta data every time a new server is added.

    private String _configFilePath;
    private ZooKeeper _zoo = null;
    private static Logger logger = Logger.getRootLogger();
    private List<ServerInfo> allServerInfo = new ArrayList<ServerInfo>();
    private Map<String, ECSNode> allNodes = new HashMap<String, ECSNode>();
    private MetaDataSet allMetadata = null;

    @Override
    public void setAllServers(String configFilePath) {
        /* put all server info from config into allServerInfo list.
        *  
        *  ecs.config example:
        *  server1 127.0.0.1 50000
        *  server2 127.0.0.1 50001
        */
       
        String line = null;
        String serverName = null;
        String serverHost = null;
        int serverPort = 0;
        String [] configInfo = new String[3];

        try {
            BufferedReader br = new BufferedReader(new FileReader(configFilePath));
            while ((line = br.readLine()) != null) {
                
                // extract server info from config file 
                // & create new serverInfo instance.
                configInfo = line.split(" ");
                serverName = configInfo[0];
                serverHost = configInfo[1];
                serverPort = Integer.parseInt(configInfo[2]);

                ServerInfo serverInfo = new ServerInfo(serverName, serverHost, serverPort);

                allServerInfo.add(serverInfo);                
            }
        } catch (Exception e) {
            logger.error("Could not set up servers from config file.");
        }        
    }

    public ECSClient(String configFilePath) {
        if (configFilePath == null || configFilePath.length() == 0)
            throw new IllegalArgumentException("configFilePath");

        _configFilePath = configFilePath;
       
        setAllServers(_configFilePath);
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
    // return the next available server that was initialized
    // from the config. if non are available, return null.

        for(int i = 0; i < allServerInfo.size(); i++) {
            if (allServerInfo.get(i).getAvailability()) {
                allServerInfo.get(i).setAvailability(false);
                return allServerInfo.get(i);
            }
        }
        return null;
    }

    @Override
    public ECSNode addNode(String cacheStrategy, int cacheSize) {
        
        ECSNode newNode = null;
        ServerInfo newServer = getNextAvailableServer();
        
        if (newServer != null) {

            // create new node using user input & available server info
            newNode = new ECSNode(newServer.getHost(), newServer.getName(), 
               newServer.getPort(), cacheStrategy, cacheSize); 

            // add the new node to hashmap allNodes.
            allNodes.put(newServer.getName(), newNode);

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

            return newNode;

        } else {
            // TODO no servers left! do something about it.
        }
        return null;
    }

    @Override
    public Collection<ECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        ECSNode newNode = null;
        ArrayList<ECSNode> newNodes = new ArrayList<ECSNode>();

        // call addNode count # of times.
        for (int i = 0; i < count; i++) {
            
            newNode = addNode(cacheStrategy, cacheSize);
            
            if (newNode != null) {
                newNodes.add(newNode);
            }
        }

        /* use CreateFromServerInfo from MetaDataSet to construct a metadata 
        *  set from a collection of server infos.
        *  TODO: sent metadata to all nodes/servers.
        */        
        
        allMetadata = MetaDataSet.CreateFromServerInfo(allServerInfo);

        return newNodes;
    }

    @Override
    public Collection<ECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
        // to be honest.. what does this do?
        // TODO
        return null;
    }

    @Override
    public boolean awaitNodes(int count, int timeout) throws Exception {
        // figure this out.
        // TODO
        return false;
    }

    public void setServerAvailable(String serverName) {
        for (int i = 0; i < allServerInfo.size(); i++) {
            if (allServerInfo.get(i).getName() == serverName) {
                allServerInfo.get(i).setAvailability(true);
            }
        }
    }

    public List<String> removeNodes(Collection<String> nodeNames) {
        // convert collection to list because easier to use.
        List<String> _nodeNames = new ArrayList<String>(nodeNames); 
        List<String> removedNodes = null;

        String nodeName = null;

        for (int i = 0; i < _nodeNames.size(); i++) {
            nodeName = _nodeNames.get(i);
            
            if (allNodes.remove(nodeName) != null) {
                removedNodes.add(nodeName);
                setServerAvailable(nodeName);
            } 
        }

        allMetadata = MetaDataSet.CreateFromServerInfo(allServerInfo);
        // TODO: send metadata to all nodes/servers.

        return removedNodes;
    }

    @Override
    public Map<String, ECSNode> getNodes() {
        return allNodes;
    }

    @Override
    public ECSNode getNodeByName(String name) {
        return allNodes.get(name);
    }

    public static void main(String[] args) {
        // Start Admin CLI
        ECSClient client = new ECSClient("ecs.config");
        client.start();
    }
}
