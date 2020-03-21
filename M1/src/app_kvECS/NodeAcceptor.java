package app_kvECS;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.Map;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.CountDownLatch;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ecs.*;
import server.*;
import java.util.*;
import shared.network.*;
import shared.metadata.*;
import shared.comms.*;
import shared.messages.KVAdminMessage;

public class NodeAcceptor extends Acceptor {
    
    private CountDownLatch _started = new CountDownLatch(1);
    private IECSClient _ecsClient;

    public NodeAcceptor(ServerSocket serverSocket, IECSClient ecsClient) {
        super(serverSocket);

        _ecsClient = ecsClient;
    }

    public void awaitStart() {
        try {
            _started.await();
        } catch (Exception e) {
            // Swallow
        }
    }

    /** 
     * Broadcasts the list of nodes to be killed.
     * Synchronous, blocks until all servers are up to date.
     */
    public List<String> sendKillMessage(List<String> nodeNames) {
        List<String> killed = new ArrayList<String>();
        synchronized(connectionsLock) {
            for (String nodeName : nodeNames) {
                boolean found = false;
                for (Connection connection : this.connections) {
                    if (found)
                        continue;

                    INodeConnection nodeConnection = (INodeConnection) connection;
                    // check if the nodeConnection is one to a node that needs 
                    // to be killed. if yes, send kill message. otherwise ignore.
                    if (nodeName.equals(nodeConnection.getNodeName())) {
                        logger.debug("Sending kill message to " + nodeName);
                        try {
                            nodeConnection.sendKillMessage();
                            killed.add(nodeName);
                        } catch (Exception e) {
                            logger.error("Failed to send kill message to " + nodeName, e);
                        }  
                        found = true;
                    }
                }

                if (!found)
                    logger.error("Could not send kill message to server " + nodeName);
            }
        }
        return killed;
    }

    public List<String> broadcastKillMessage() {
        List<String> killed = new ArrayList<String>();
        synchronized(connectionsLock) {
            for (Connection connection : this.connections) {
                INodeConnection nc = (INodeConnection) connection;
                logger.debug("Sending kill message to " + nc.getNodeName());
                try {
                    nc.sendKillMessage();
                    killed.add(nc.getNodeName());
                } catch (Exception e) {
                    logger.error("Failed to send kill message", e);
                }
            }
        }
        return killed;
    }

    /** 
     * Broadcasts the metadata set to all active connections.
     * Synchronous, blocks until all servers are up to date.
     */
    public void broadcastMetadata(MetaDataSet mds) {
        if (mds == null)
            throw new IllegalArgumentException("mds is null");

        logger.debug("Broadcasting metadata: " + mds.toString());

        synchronized(connectionsLock) {
            for (Connection connection : this.connections) {
                INodeConnection nodeConnection = (INodeConnection) connection;
                try {
                    nodeConnection.sendMetadata(mds);
                } catch (Exception e) {
                    logger.error("Failed to send metadata", e);
                }
            }
        }
    }

    public void sendMetadata(MetaDataSet mds, String targetServerName) {
        logger.debug("sending metadata to " + targetServerName);
        synchronized(connectionsLock) {
            INodeConnection matchingConnection = 
                    getConnectionWithName(targetServerName);
            if (matchingConnection == null) {
                logger.error(
                    new Exception("Could not find matching server for " + 
                        "transfer request")
                );
                return;
            }

            try {
                matchingConnection.sendMetadata(mds);
                logger.debug("Metadata sent and recv'd");
            } catch (Exception e) {
                logger.error("Failed to send metadata", e);
            }
        }
    }

    /** 
     * Sends a transfer request to the appropriate node.
     * Synchronous, blocks until the transfer is complete.
     */
    public KVAdminMessage sendTransferRequest(TransferRequest tr) {
        logger.debug("sending transfer request");
        String sourceName = tr.getFromName();
        KVAdminMessage transferStatus = null;

        synchronized(connectionsLock) {
            INodeConnection matchingConnection = 
                    getConnectionWithName(sourceName);

            if (matchingConnection == null) {
                logger.error(new Exception("Could not find matching server for "
                        + "transfer request"));
                return new KVAdminMessage(KVAdminMessage.StatusType.TRANSFER_REQUEST_FAILURE, null);
            }

            try {
                transferStatus = matchingConnection.sendTransferRequest(tr);
            } catch (Exception e) {
                logger.error("Failed to send transfer request", e);
            }
        }

        return transferStatus;
    } 

    private INodeConnection getConnectionWithName(String name) {
        // Assumes that the connectionsLock is currently held
        for (Connection c : this.connections) {
            INodeConnection nodeConnection = (INodeConnection) c;

            if (nodeConnection.getNodeName().equals(name))
                return nodeConnection;

        }
        return null;
    }

    @Override
    public Connection handleConnection(Socket clientSocket) {
        // Determine the node's name using zookeeper
        try {
            // Read the node name
            String nodeName = new String(new CommChannel(clientSocket).recvBytes());
            logger.debug("Node connected: " + nodeName);
            return new NodeConnection(clientSocket, this, nodeName);
        } catch (Exception e) {
            logger.error("handleconnection error", e);
            return null;
        }
    }

    // private String getNodeName(Socket nodeConnectionSocket) throws Exception {
    //     if (nodeConnectionSocket == null)
    //         throw new IllegalArgumentException("node connection socket is null");
        
    //     int port = nodeConnectionSocket.getPort();

    //     logger.debug("searching for node on port " + port);

    //     for (Map.Entry<String, ECSNode> entry : this._ecsClient.getNodes().entrySet()) {
    //         logger.debug("")
    //         if (entry.getValue().getNodePort() == port) {
    //             return entry.getKey();
    //         }
    //     }
       
    //     throw new Exception("Could not find server on port " + port); 
    // }

    @Override
    public void run() {
        this.running = true;
        logger.info("Node acceptor running\n");
        CountDownLatch runningLatch = this._started;
        while (isRunning()) {
            try {

                // Hopefully this race condition will never happen :)
                if (runningLatch != null) {
                    runningLatch.countDown();
                    runningLatch = null;
                }
                Socket clientSocket = this.serverSocket.accept();

                Connection connection = handleConnection(clientSocket);

                //add the connection to the client list
                synchronized(connectionsLock) {
                    connections.add(connection);
                }
                // Signal to ECS that the server is connected
                String connectedNodeName = ((NodeConnection) connection).getNodeName();
                _ecsClient.signalNodeConnected(connectedNodeName);

                logger.info("Accepted connection from " 
                    + clientSocket.getInetAddress().getHostName() 
                    + " on port " + clientSocket.getPort()
                );
            } catch (IOException ioe) {
                logger.warn("Unable to accept connection. Stopping node acceptor.", ioe);
                this.running = false;
            }
        }
    }
}
