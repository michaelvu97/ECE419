package app_kvECS;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import server.*;
import shared.network.*;
import shared.metadata.*;

public class NodeAcceptor extends Acceptor {

    private int _numServersRemaining;

    public NodeAcceptor(ServerSocket serverSocket, int numKVServers) {
        super(serverSocket);

        if (numKVServers <= 0) {
            throw new IllegalArgumentException("numKVServers out of range: " 
                + numKVServers);
        }

        _numServersRemaining = numKVServers;
    }

    /** 
     * Broadcasts the metadata set to all active connections.
     * Synchronous, blocks until all servers are up to date.
     */
    public void broadcastMetadata(MetaDataSet mds) {
        if (mds == null)
            throw new IllegalArgumentException("mds is null");

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
            } catch (Exception e) {
                logger.error("Failed to send metadata", e);
            }
        }
    }

    /** 
     * Sends a transfer request to the appropriate node.
     * Synchronous, blocks until the transfer is complete.
     */
    public void sendTransferRequest(TransferRequest tr) {

        String sourceName = tr.getFromName();

        synchronized(connectionsLock) {
            // TODO find the server that matches this name
            INodeConnection matchingConnection = 
                    getConnectionWithName(sourceName);

            if (matchingConnection == null) {
                logger.error(new Exception("Could not find matching server for "
                        + "transfer request"));
                return;
            }

            try {
                matchingConnection.sendTransferRequest(tr);
            } catch (Exception e) {
                logger.error("Failed to send transfer request", e);
            }
        }
    } 

    private INodeConnection getConnectionWithName(String name) {
        // TODO
        throw new IllegalStateException("NOT IMPLEMENETED");
    }

    @Override
    public Connection handleConnection(Socket clientSocket) {
        return new NodeConnection(clientSocket, this);
    }

    @Override
    public void run() {
        this.running = true;
        while (isRunning()) {
            try {
                Socket clientSocket = this.serverSocket.accept();

                Connection connection = handleConnection(clientSocket);

                //add the connection to the client list
                synchronized(connectionsLock) {
                    connections.add(connection);
                    _numServersRemaining--;

                    if (_numServersRemaining == 0) {
                        
                        logger.info("All KV Servers connected, starting "
                            + "connection threads");
                    }
                }

                // Note that the handler does not start until all servers are
                // connected

                logger.info("Accepted connection from " 
                    + clientSocket.getInetAddress().getHostName() 
                    + " on port " + clientSocket.getPort()
                );
            } catch (IOException ioe) {
                logger.error("Unable to accept connection", ioe);
            }
        }
    }
}
