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
import shared.ZooKeeperConstants;
import org.apache.zookeeper.*;
import shared.messages.KVAdminMessage;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;

public class ECSClient implements IECSClient {

    private int _port = 0;
    private String _host = null;
    private ZooKeeper _zooKeeper = null;
    private String _username = null;
    private int ecsClientPort = 6969;
    private ServerSocket ecsSocket;
    private String _configFilePath = null;
    private MetaDataSet allMetadata = null;
    private NodeAcceptor nodeAcceptor = null;
    private static Logger logger = org.apache.log4j.Logger.getRootLogger();
    private List<ServerInfo> allServerInfo = new ArrayList<ServerInfo>();
    private Map<String, ECSNode> allNodes = new HashMap<String, ECSNode>();
    private Set<String> runningNodes = new HashSet<String>();

    private CountDownLatch _zkConnectionLatch = new CountDownLatch(1);
    private CountDownLatch _waitForServerToConnectBack = new CountDownLatch(1);

    private INodeFailureDetector _nodeFailureDetector;

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
        _username = username != null ? username : System.getProperty("user.name");

        setAllServers(_configFilePath);
    }

    @Override
    public boolean start() {
        logger.info("Initializing ECS Client...");
        
        try {
			ecsSocket = new ServerSocket(ecsClientPort);
			
			_port = ecsSocket.getLocalPort();

			// _host = ecsSocket.getInetAddress().getHostName();
            _host = InetAddress.getLocalHost().getHostAddress();

			logger.info("ECSClient " + _host + " listening on port: " + 
                    ecsSocket.getLocalPort());    
            
             logger.info("Connecting to ZK");

            _zooKeeper = new ZooKeeper(_host + ":" + ZooKeeperConstants.ZK_PORT,
                ZooKeeperConstants.TIMEOUT,
                new Watcher() {
                    @Override
                    public void process(WatchedEvent we) {
                        if (we.getState() == 
                                    Watcher.Event.KeeperState.SyncConnected) {
                            try {
                                String path = _zooKeeper.create(
                                    ZooKeeperConstants.APP_FOLDER,
                                    "this_is_the_app_folder".getBytes(),
                                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                    CreateMode.PERSISTENT
                                );
                                logger.info("Created zk path: " + path);
                            } catch (Exception e) {
                                logger.info("Could not register app", e);
                            }
                            _zkConnectionLatch.countDown();
                            return;
                        }
                    }
                }
            );

            logger.info("Connected to ZK");

            try {
                _zkConnectionLatch.await();
            } catch (Exception e) {
                logger.warn("oopsie");
            }

            _nodeFailureDetector = new NodeFailureDetector(
                    _host + ":" + ZooKeeperConstants.ZK_PORT,
                    ZooKeeperConstants.APP_FOLDER
            );
            _nodeFailureDetector.addNodeFailedListener(this);
            new Thread(_nodeFailureDetector).start();
            _nodeFailureDetector.awaitStart();

            nodeAcceptor = new NodeAcceptor(ecsSocket, this);
            new Thread(nodeAcceptor).start();
            nodeAcceptor.awaitStart();

            logger.info("All ECS startup threads are running");
            
            return true;
		}
		catch (IOException e) {
        	logger.error("Error! Cannot open server socket:");
            if(e instanceof BindException){
            	logger.error("Port " + _port + " is already bound! Requested port: " + ecsClientPort);
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
        // TODO: kill all servers
        try {

            String primaryServerName = allServerInfo.get(0).getName();

            if (allServerInfo.get(0).getAvailability()) {
                // Need to add the first server to collapse all of the data.
                addNode("FIFO", 1);
            }

            ecsSocket.close();
            logger.info("ECSClient socket is closed.");
            nodeAcceptor.stop();
            _nodeFailureDetector.stop();

            List<String> stillActive = getActiveNodeNames();

            if (stillActive.contains(primaryServerName)) {
                stillActive.remove(primaryServerName);
            }

            removeNodes(stillActive);

            // Confirm that the last node is server_1
            stillActive = getActiveNodeNames();
            if (stillActive.size() != 1) {
                logger.error("Invalid number of active nodes!");
            } else {
                if (allServerInfo.get(0).getAvailability()) {
                    logger.error("Final node is not the last one! The last still-alive node is " + stillActive.get(0));
                } else {
                    logger.info("Removing final node");
                    removeFinalNode();
                }
            }

            logger.info("broadcasting final kill");
            nodeAcceptor.broadcastKillMessage();

            return true;
        } catch (IOException e) {
            logger.error("Error! " + "Unable to close socket on port: " + _port, e);
            return false;
        }
    }

    private void initializeZkApp() {

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
    public synchronized ECSNode addNode(String cacheStrategy, int cacheSize) {
        MetaDataSet oldMetaData = null;
        KVAdminMessage transferStatus = null;

        ServerInfo newServer = null;

        if (getActiveNodes().size() > 0) {
            // Old metadata existed.
            oldMetaData = MetaDataSet.CreateFromServerInfo(getActiveNodes());
        } else {
            // This is the first server
        }

        newServer = getNextAvailableServer();


        ECSNode newNode = null;
        
        if (newServer == null) {
            logger.error("No more available servers");
            return null;
        }

        logger.info("Attempting to add server: " + newServer.getName());

        newNode = new ECSNode(newServer.getHost(), newServer.getName(), 
                newServer.getPort(), cacheStrategy, cacheSize); 
        
        if (!runningNodes.contains(newServer.getName())) {

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

            logger.info("Running KVServer start script");
            try {
                proc = run.exec(cmd);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            // Block until the server connects.
            try {
                _waitForServerToConnectBack.await();
            } catch (Exception e) {
                logger.error("Server connection callback interrupted, may not be connected", e);
            }
            logger.info("KVServer connected");

            runningNodes.add(newServer.getName());

        }

        // The new server is now connected.
        List<ServerInfo> activeNodes = getActiveNodes();

        // Recalculate metadata
        MetaDataSet newMetadata = MetaDataSet.CreateFromServerInfo(activeNodes);

        logger.debug(oldMetaData + ", " + activeNodes.size());

        if (oldMetaData != null && activeNodes.size() != 1) {
            // Other nodes exist, transfer will be required.
            logger.info("Deleting any old data");

            nodeAcceptor.sendDeleteData(newServer.getName());

            logger.info("Starting transfer");

            // Tell the new node the new metadata
            nodeAcceptor.sendMetadata(newMetadata, newServer.getName());

            // See who shrunk
            MetaData shrunkboy = oldMetaData.getPrimaryForHash(
                    HashUtil.ComputeHash(
                        newServer.getHost(),
                        newServer.getPort()
                    )
            );

            if (shrunkboy == null) {
                logger.error("The shrunk server is null! New server= " + newServer.getName());

            } else 

            logger.info("sending transfer request from " + shrunkboy.getName() + " to " + newServer.getName());

            // Send shrunk server a transfer request to the new server
            transferStatus = nodeAcceptor.sendTransferRequest(
                new TransferRequest(
                        shrunkboy.getName(),
                        newServer.getName(),
                        newMetadata
                    )
            );
     
            // Do something with transferStatus.
            if (transferStatus.getStatus() == KVAdminMessage.StatusType.TRANSFER_REQUEST_FAILURE) {
                logger.warn("ECS: could not complete server transfer request.");

                // return false;
            } else if (transferStatus.getStatus() == KVAdminMessage.StatusType.TRANSFER_REQUEST_SUCCESS) {

                // Broadcast new metadata.
                nodeAcceptor.broadcastMetadata(newMetadata);
                
                // Everyone now has the new metadata, and all data is transferred onto
                // the new server.
                return newNode;
            } else {
                logger.error("Invalid transferstatus: " + transferStatus.getStatus());
            }
        } else if (oldMetaData == null) {
            // This is the first node to connect
            logger.info("Sending initial metadata");
            nodeAcceptor.sendMetadata(newMetadata, newServer.getName());
        }
        // TODO: default return?
        return newNode;
    }

    @Override
    public synchronized List<ECSNode> addNodes(int count, String cacheStrategy,
            int cacheSize) {
        logger.info("ECS: Adding " + count + " nodes");

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
    public synchronized boolean awaitNodes(int count, int timeout) 
            throws Exception {
        nodeAcceptor.broadcastMetadata(MetaDataSet.CreateFromServerInfo(getActiveNodes()));
        return true;
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

    private List<String> getActiveNodeNames() {
        List<ServerInfo> active = getActiveNodes();
        List<String> result = new ArrayList<String>();
        for (ServerInfo s : active) {
            result.add(s.getName());
        }

        return result;
    }

    private void setServerAvailable(String serverName) {
        for (int i = 0; i < allServerInfo.size(); i++) {
            if (allServerInfo.get(i).getName().equals(serverName)) {
                allServerInfo.get(i).setAvailability(true);
            }
        }
    }

    private synchronized void removeFinalNode() {
        nodeAcceptor.sendCloseMessage(allServerInfo.get(0).getName());
    }

    @Override
    public synchronized boolean removeNode(String nodeName) {
        KVAdminMessage transferStatus = null;

        if (getActiveNodes().size() == 1) {
            // If we're the only node, deny
            // Can't remove the last node.
            logger.error("Can't remove the last node in the cluster.");
            return false;
        }

        // Detect who will grow
        MetaDataSet oldMetaData = MetaDataSet.CreateFromServerInfo(getActiveNodes());
        setServerAvailable(nodeName);
        MetaDataSet newMetaData = MetaDataSet.CreateFromServerInfo(getActiveNodes());

        ECSNode nodeToDelete = getNodeByName(nodeName);
        allNodes.remove(nodeName);

        MetaData growboy = newMetaData.getPrimaryForHash(
            HashUtil.ComputeHash(
                nodeToDelete.getNodeHost(),
                nodeToDelete.getNodePort()
            )
        );

        // Tell removed node to transfer all to the growing node
        transferStatus = nodeAcceptor.sendTransferRequest(new TransferRequest(
            nodeName,
            growboy.getName(),
            newMetaData
        ));

        // do something with transferStatus.
        if (transferStatus.getStatus() == KVAdminMessage.StatusType.TRANSFER_REQUEST_FAILURE) {
            logger.warn("ECS: could not complete server transfer request.");

            return false;
        } else if (transferStatus.getStatus() == KVAdminMessage.StatusType.TRANSFER_REQUEST_SUCCESS) {
            // Broadcast metadata update
            nodeAcceptor.broadcastMetadata(newMetaData);
            return true;
        } else {
            logger.error("Unknown transfer request response: " + transferStatus.getStatus());
            return false;
        }
    }

    public synchronized List<String> removeNodes(List<String> nodeNames) {
        String nodeName = null;
        List<String> removedNodes = new ArrayList<String>();

        logger.info("Removing " + nodeNames.size() + " nodes");

        for (int i = 0; i < nodeNames.size(); i++) {
            nodeName = nodeNames.get(i);
            
            if (allNodes.containsKey(nodeName)) {
                if (removeNode(nodeName)) {
                    removedNodes.add(nodeName);    
                }
            } 
        }

        return removedNodes;
    }

    @Override
    public List<String> killNodes(List<String> nodeNames) {
        return nodeAcceptor.sendKillMessage(nodeNames); 
    }

    @Override
    public boolean killNode(String nodeName) {
        List<String> killList = new ArrayList<String>();
        killList.add(nodeName);
        return killNodes(killList).size() != 0;
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

    @Override
    public synchronized void onNodeFailed(String nodeName) {
        logger.error("NODE FAILED: " + nodeName);
        
        // TODO: HANDLE ERRORS

        // Detect who will grow
        MetaDataSet oldMetaData = MetaDataSet.CreateFromServerInfo(getActiveNodes());
        setServerAvailable(nodeName);
        runningNodes.remove(nodeName);
        MetaDataSet newMetaData = MetaDataSet.CreateFromServerInfo(getActiveNodes());

        ECSNode nodeToDelete = getNodeByName(nodeName);
        allNodes.remove(nodeName);

        // MetaData growboy = newMetaData.getServerForHash(
        //     HashUtil.ComputeHash(
        //         nodeToDelete.getNodeHost(),
        //         nodeToDelete.getNodePort()
        //     )
        // );

    // Inform nodeAcceptor that a connection has died.
        nodeAcceptor.onNodeFailed(nodeName);

        // Broadcast metadata update
        nodeAcceptor.broadcastMetadata(newMetaData);

        // TODO: add a new node?
        logger.error("ECSClient.onNodeFailed still does not spawn a new node to replace the failed one");
    }

    public static void main(String configFile, String username) {
        ECSClient client = new ECSClient(configFile, username);
        client.start();
    }
}
