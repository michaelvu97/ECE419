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

                        // Start all connections
                        for (Connection c : connections) {
                            new Thread(c).start();
                        }

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
