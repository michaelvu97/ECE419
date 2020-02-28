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
    private ServerSocket _serverSocket;
    
    private Object _connectionsLock = new Object();
    private ArrayList<Connection> _connections = new ArrayList<Connection>();

    private boolean _running = false;

    protected static Logger logger = Logger.getRootLogger();

    protected Acceptor(ServerSocket serverSocket) {
        if (serverSocket == null)
            throw new IllegalArgumentException("server socket is null");

        this._serverSocket = serverSocket;
    }
    
    public boolean isRunning() {
        return this._running;
    }

    public void alertClose(Connection connectionToClose) {
        synchronized(_connectionsLock) {
            for (int i = 0; i < _connections.size(); i++){
                if(connectionToClose == _connections.get(i)){
                    _connections.remove(i);
                }
            }
        }
        return;
    }

    public void stop() {
        //Send kill message to client handler threads.
        for(int i = 0; i < _connections.size(); i++){
            Connection connection = _connections.get(i);
            connection.kill();
        }

        //5 second timeout on close
        int i = 0;
        while (_connections.size() != 0 && i != 500){
            i++;
            try {
                Thread.sleep(10);
            } catch (Exception InterruptedException){
                //if we cant sleep, just go
            } 
        } 

        this._running = false;
    }

    public abstract Connection handleConnection(Socket clientSocket);
     
    @Override
    public void run() {
        this._running = true;
        while (isRunning()) {
            try {
                Socket clientSocket = this._serverSocket.accept();

                Connection connection = handleConnection(clientSocket);

                //add the connection to the client list
                synchronized(_connectionsLock) {
                    _connections.add(connection);
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
