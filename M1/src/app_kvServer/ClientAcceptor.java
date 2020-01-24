package app_kvServer;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import server.*;

public class ClientAcceptor implements Runnable {
    private ServerSocket _serverSocket;
    private IServerStore _serverStore;

    private boolean _running = false;

    private static Logger logger = Logger.getRootLogger();

    public ClientAcceptor(ServerSocket serverSocket, IServerStore serverStore) {
        super();
        if (serverSocket == null)
            throw new IllegalArgumentException("server socket is null");
        if (serverStore == null)
            throw new IllegalArgumentException("server store is null");

        this._serverStore = serverStore;
        this._serverSocket = serverSocket;
    }
    
    public boolean isRunning() {
        return this._running;
    }

    public void stop() {
        this._running = false;

        // TODO: kill client handler threads.
    }
     
    @Override
    public void run() {
        this._running = true;
        while (isRunning()) {
            try {
                Socket clientSocket = this._serverSocket.accept();

                ClientConnection clientConnection = new ClientConnection(
                        clientSocket,
                        this._serverStore
                );

                new Thread(clientConnection).start();

                logger.info("Connected to " + clientSocket.getInetAddress().getHostName() + " on port " + clientSocket.getPort());
            } catch (IOException ioe) {
                logger.error("Unable to establish connection", ioe);
            }
        }
    }
}
