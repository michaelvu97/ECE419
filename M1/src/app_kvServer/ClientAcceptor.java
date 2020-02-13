package app_kvServer;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import server.*;

public class ClientAcceptor implements Runnable {
    private ServerSocket _serverSocket;
    private IServerStore _serverStore;
    private IMetaDataManager _metaDataManager;
    
    private ArrayList <ClientConnection> clientList = new ArrayList<ClientConnection>();  
    private Object _lock = new Object();
    	    
    private boolean _running = false;

    private static Logger logger = Logger.getRootLogger();

    public ClientAcceptor(ServerSocket serverSocket, IServerStore serverStore,
            IMetaDataManager metaDataManager) {
        
        super();
        if (serverSocket == null)
            throw new IllegalArgumentException("server socket is null");
        if (serverStore == null)
            throw new IllegalArgumentException("server store is null");
        if (metaDataManager == null)
            throw new IllegalArgumentException("metaDataManager is null");

        this._serverStore = serverStore;
        this._serverSocket = serverSocket;
        this._metaDataManager = metaDataManager;
    }
    
    public boolean isRunning() {
        return this._running;
    }

    public void alertClose (ClientConnection toClose) {
        synchronized(_lock) {
            for (int i = 0; i < clientList.size(); i++){
                if(toClose == clientList.get(i)){
                    clientList.remove(i);
                }
            }
	    }
        return;
    }

    public void stop() {
        //Send kill message to client handler threads.
        for(int i = 0; i < clientList.size(); i++){
            ClientConnection connection = clientList.get(i);
            connection.kill();
        }

        //5 second timeout on close
        int i = 0;
        while (clientList.size()!=0 && i!=500){
            i++;
            try {
                Thread.sleep(10);
            } catch (Exception InterruptedException){
                //if we cant sleep, just go
            } 
        } 

        this._running = false;
    }
     
    @Override
    public void run() {
        this._running = true;
        while (isRunning()) {
            try {
                Socket clientSocket = this._serverSocket.accept();

                ClientConnection clientConnection = new ClientConnection(
                        clientSocket,
                        this._serverStore,
                        this._metaDataManager,
                        this
                );

                //add the connection to the client list
                synchronized(_lock) {
                    clientList.add(clientConnection);
                }

                new Thread(clientConnection).start();

                logger.info("Connected to " + clientSocket.getInetAddress().getHostName() + " on port " + clientSocket.getPort());
            } catch (IOException ioe) {
                logger.error("Unable to establish connection", ioe);
            }
        }
    }
}
