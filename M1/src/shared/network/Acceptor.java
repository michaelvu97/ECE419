package shared.network;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import server.*;

/**
 * IOC abstract class for handling nodes/clients connecting to this process.
 */
public abstract class Acceptor implements Runnable {
    protected ServerSocket serverSocket;
    
    protected Object connectionsLock = new Object();
    protected ArrayList<Connection> connections = new ArrayList<Connection>();

    protected boolean running = false;

    protected static Logger logger = Logger.getRootLogger();

    protected Acceptor(ServerSocket serverSocket) {
        if (serverSocket == null)
            throw new IllegalArgumentException("server socket is null");

        this.serverSocket = serverSocket;
    }
    
    public boolean isRunning() {
        return this.running;
    }

    public void alertClose(Connection connectionToClose) {
        synchronized(connectionsLock) {
            for (int i = 0; i < connections.size(); i++){
                if (connectionToClose.equals(connections.get(i))){
                    connections.remove(i);
                }
            }
        }
        return;
    }

    public void stop() {
        this.running = false;

        synchronized(connectionsLock) {

            //Send kill message to client handler threads.
            for(int i = 0; i < connections.size(); i++){
                Connection connection = connections.get(i);
                connection.kill();
            }
        }

        //5 second timeout on close
        // int i = 0;
        // while (connections.size() != 0 && i != 500){
        //     i++;
        //     try {
        //         Thread.sleep(10);
        //     } catch (Exception InterruptedException){
        //         //if we cant sleep, just go
        //     } 
        // } 

    }

    public abstract Connection handleConnection(Socket clientSocket);
     
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
                }

                new Thread(connection).start();

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
