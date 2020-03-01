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
import shared.network.*;

public final class ClientAcceptor extends Acceptor {
    private IKVServer _kvServer;
    private IServerStore _serverStore;
    private IMetaDataManager _metaDataManager;
    
    public ClientAcceptor(ServerSocket serverSocket, IServerStore serverStore,
            IMetaDataManager metaDataManager,
            IKVServer kvServer) {
        
        super(serverSocket);

        if (serverStore == null)
            throw new IllegalArgumentException("server store is null");
        if (metaDataManager == null)
            throw new IllegalArgumentException("metaDataManager is null");

        this._serverStore = serverStore;
        this._metaDataManager = metaDataManager;
        this._kvServer = kvServer;
    }
    
    @Override
    public Connection handleConnection(Socket clientSocket) {
        return new ClientConnection(
                clientSocket,
                this._serverStore,
                this._metaDataManager,
                this,
                this._kvServer
        );
    }
}
