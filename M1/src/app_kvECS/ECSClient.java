package app_kvECS;

import ecs.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.nio.file.*;
import java.nio.charset.*;
import java.io.IOException;

import shared.metadata.*;
import org.apache.zookeeper.*;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.InetAddress;

public class ECSClient implements IECSClient {

    private int ecsClientPort = 6969;
    private int _port = 0;
    private String _host = null;
    private ServerSocket _ecsSocket;
    private String _configFilePath;
    private ZooKeeper _zoo = null;
    private static Logger logger = Logger.getRootLogger();
    private List<ServerInfo> allServerInfo = new ArrayList<ServerInfo>();
    private Map<String, ECSNode> allNodes = new HashMap<String, ECSNode>();
    private MetaDataSet allMetadata = null;
    private NodeAcceptor nodeAcceptor = null;

    private String _username;

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
        if (username == null || username.length() == 0)
            throw new IllegalArgumentException("username");

        _configFilePath = configFilePath;
        _username = username;

        setAllServers(_configFilePath);
    }

    @Override
    public boolean start() {
        // i have no idea if this is right 
        logger.info("Initializing ECS Client...");
        
        try {
			_ecsSocket = new ServerSocket(ecsClientPort);
			
			_port = _ecsSocket.getLocalPort();
			_host = _ecsSocket.getInetAddress().getHostName();

			logger.info("ECSClient " + _host + " listening on port: " + _ecsSocket.getLocalPort());    
            
            nodeAcceptor = new NodeAcceptor(_ecsSocket, this);
            
            return true;
		}
		catch (IOException e) {
        	logger.error("Error! Cannot open server socket:");
            if (e instanceof BindException){
            	logger.error("Port " + _port + " is already bound!");
            }
            return false;
        }
    }

    @Override
    public boolean stop() {
        try {
            _ecsSocket.close();
            logger.info("Server socket is closed.");
            return true;
        } catch (IOException e) {
            logger.error("Error! " + "Unable to close socket on port: " + _port, e);
            return false;
        }
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
            Process proc = null;
            String script = "kv_server.sh";

            Runtime run = Runtime.getRuntime();
            
            String cmd[] = {
                script,
                _username,
                newServer.getHost(), // config host
                newServer.getName(), // server name
                Integer.toString(newServer.getPort()), // server port
                cacheStrategy, 
                Integer.toString(cacheSize),
                newServer.getName(), // disk storage string
                _host,           // ecs hostname
                Integer.toString(_port), // ecs port
            };

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

        /* use CreateFromServerInfo from MetaDataSet to construct a 
        *  metadata set from a collection of server infos.
        *  sent metadata to all nodes/servers.
        */        
        
        allMetadata = MetaDataSet.CreateFromServerInfo(allServerInfo);
        nodeAcceptor.broadcastMetadata(allMetadata);

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

    public void setServerAvailable(String serverName) {
        for (int i = 0; i < allServerInfo.size(); i++) {
            if (allServerInfo.get(i).getName() == serverName) {
                allServerInfo.get(i).setAvailability(true);
            }
        }
    }

    public List<String> removeNodes(List<String> nodeNames) {
        String nodeName = null;
        List<String> removedNodes = null;

        for (int i = 0; i < nodeNames.size(); i++) {
            nodeName = nodeNames.get(i);
            
            if (allNodes.remove(nodeName) != null) {
                removedNodes.add(nodeName);
                setServerAvailable(nodeName);
            } 
        }

        allMetadata = MetaDataSet.CreateFromServerInfo(allServerInfo);
        nodeAcceptor.broadcastMetadata(allMetadata);

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

    public static void main(String configFile, String username) {
        ECSClient client = new ECSClient(configFile, username);
        client.start();
    }
}
