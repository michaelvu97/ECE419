package app_kvECS;

import ecs.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;

import java.net.BindException;
import java.net.InetAddress;

import shared.metadata.*;
import org.apache.zookeeper.*;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;

public class ECSClient implements IECSClient {

    private int _port = 0;
    private String _host = null;
    private ZooKeeper _zoo = null;
    private String _username = null;
    private int ecsClientPort = 6969;
    private ServerSocket ecsSocket;
    private String _configFilePath = null;
    private MetaDataSet allMetadata = null;
    private NodeAcceptor nodeAcceptor = null;
    private static Logger logger = Logger.getRootLogger();
    private List<ServerInfo> allServerInfo = new ArrayList<ServerInfo>();
    private Map<String, ECSNode> allNodes = new HashMap<String, ECSNode>();

    private CountDownLatch _waitForServerToConnectBack = new CountDownLatch(1);

    @Override  
    public List<ServerInfo> getAllServerInfo(){
        return allServerInfo;
    }
    
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

    public ECSClient(String configFilePath, String username) {
        if (configFilePath == null || configFilePath.length() == 0)
            throw new IllegalArgumentException("configFilePath");

        _configFilePath = configFilePath;
        _username = username;

        setAllServers(_configFilePath);
    }

    @Override
    public boolean start() {
        logger.info("Initializing ECS Client...");
        
        try {
			ecsSocket = new ServerSocket(ecsClientPort);
			
			_port = ecsSocket.getLocalPort();
			_host = ecsSocket.getInetAddress().getHostName();

			logger.info("ECSClient " + _host + " listening on port: " + ecsSocket.getLocalPort());    
            
            nodeAcceptor = new NodeAcceptor(ecsSocket, this);
            new Thread(nodeAcceptor).start();
            
            return true;
		}
		catch (IOException e) {
        	logger.error("Error! Cannot open server socket:");
            if(e instanceof BindException){
            	logger.error("Port " + _port + " is already bound!");
            }
            return false;
        }
    }

    @Override
    public boolean stop() {
      // stop is implemented in the remove function
      return true;
    }

    @Override
    public boolean shutdown() {
        try {
            ecsSocket.close();
            logger.info("ECSClient socket is closed.");
            nodeAcceptor.stop();
            return true;
        } catch (IOException e) {
            logger.error("Error! " + "Unable to close socket on port: " + _port, e);
            return false;
        }
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
        MetaDataSet oldMetaData = null;
        if (getActiveNodes().size() != 0) {
            // This is the first ever node.
            oldMetaData = MetaDataSet.CreateFromServerInfo(getActiveNodes());    
        }

        ECSNode newNode = null;
        
        ServerInfo newServer = getNextAvailableServer();
        
        if (newServer == null) {
            logger.error("No more available servers");
            return null;
        }

        // create new node using user input & available server info
        newNode = new ECSNode(newServer.getHost(), newServer.getName(), 
             newServer.getPort(), cacheStrategy, cacheSize); 

        // Set the countdown latch.
        _waitForServerToConnectBack = new CountDownLatch(1);

        // add the new node to hashmap allNodes.
        allNodes.put(newServer.getName(), newNode);

        // ssh call to start KV server
        Process proc = null;
        String script = "./src/app_kvECS/kv_server.sh";

        Runtime run = Runtime.getRuntime();
        
        String cmd[] = {
            script,
            _username,
            newServer.getHost(), // config host
            newServer.getName(), // server name
            Integer.toString(newServer.getPort()), // server port
            cacheStrategy, 
            Integer.toString(cacheSize),
            _host,           // ecs hostname
            Integer.toString(_port),               // ecs port
        };

        try {
            proc = run.exec(cmd);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Block until the server connects.
        try {
            _waitForServerToConnectBack.await();
        } catch (Exception e) {
            logger.error("Server conenction callback interrupted, may not be connected", e);
        }

        // The new server is now connected.
        List<ServerInfo> activeNodes = getActiveNodes();

        // Recalculate metadata
        MetaDataSet newMetadata = MetaDataSet.CreateFromServerInfo(activeNodes);

        if (oldMetaData != null && activeNodes.size() != 1) {
            // Other nodes exist, transfer will be required.

            // Tell the new node the new metadata
            nodeAcceptor.sendMetadata(newMetadata, newServer.getName());

            // See who shrunk
            MetaData shrunkboy = oldMetaData.getServerForHash(
                    HashUtil.ComputeHash(
                        newServer.getHost(),
                        newServer.getPort()
                    )
            );

            // Send shrunk server a transfer request to the new server
            nodeAcceptor.sendTransferRequest(
                    new TransferRequest(
                        shrunkboy.getName(),
                        newServer.getName(),
                        newMetadata
                    )
            );
        }
        
        // Broadcast new metadata.
        nodeAcceptor.broadcastMetadata(newMetadata);
        
        // Everyone now has the new metadata, and all data is transferred onto
        // the new server.
        return newNode;
    }

    @Override
    public List<ECSNode> addNodes(int count, String cacheStrategy, int cacheSize) {
        ECSNode newNode = null;
        ArrayList<ECSNode> newNodes = new ArrayList<ECSNode>();

        // call addNode count # of times.
        for (int i = 0; i < count; i++) {
            
            newNode = addNode(cacheStrategy, cacheSize);
            
            if (newNode != null) {
                newNodes.add(newNode);
            }
        }
        
        logger.debug(allServerInfo.size());

        return newNodes;
    }

    @Override
    public List<ECSNode> setupNodes(int count, String cacheStrategy, int cacheSize) {
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

    private List<ServerInfo> getActiveNodes() {
        List<ServerInfo> result = new ArrayList<ServerInfo>();
        for (ServerInfo s : allServerInfo) {
            if (s.getAvailability())
                continue;

            result.add(s);
        }

        return result;
    }

    public void setServerAvailable(String serverName) {
        for (int i = 0; i < allServerInfo.size(); i++) {
            if (allServerInfo.get(i).getName().equals(serverName)) {
                allServerInfo.get(i).setAvailability(true);
            }
        }
    }

    public void removeNode(String nodeName) {
        if (false) {
            // If we're the only node, deny
            // Can't remove the last node.
            logger.error("TODO REMOVE NODE ON THE LAST NODE");
        }

        // Detect who will grow
        MetaDataSet oldMetaData = MetaDataSet.CreateFromServerInfo(getActiveNodes());
        setServerAvailable(nodeName);
        MetaDataSet newMetaData = MetaDataSet.CreateFromServerInfo(getActiveNodes());

        ECSNode nodeToDelete = getNodeByName(nodeName);
        allNodes.remove(nodeName);

        MetaData growboy = newMetaData.getServerForHash(
            HashUtil.ComputeHash(
                nodeToDelete.getNodeHost(),
                nodeToDelete.getNodePort()
            )
        );

        // Tell removed node to transfer all to the growing node
        nodeAcceptor.sendTransferRequest(new TransferRequest(
            nodeName,
            growboy.getName(),
            newMetaData
        ));

        // Broadcast metadata update
        nodeAcceptor.broadcastMetadata(newMetaData);
    }

    public List<String> removeNodes(List<String> nodeNames) {
        String nodeName = null;
        List<String> removedNodes = new ArrayList<String>();

        for (int i = 0; i < nodeNames.size(); i++) {
            nodeName = nodeNames.get(i);
            
            if (allNodes.containsKey(nodeName)) {
                removedNodes.add(nodeName);
                removeNode(nodeName);
            } 
        }

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

    @Override
    public void signalNodeConnected(String name) {
        // Find the server with that name, mark is as online?

        _waitForServerToConnectBack.countDown();
    }

    public static void main(String configFile, String username) {
        ECSClient client = new ECSClient(configFile, username);
        client.start();
    }
}
