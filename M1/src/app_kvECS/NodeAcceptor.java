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

public class NodeAcceptor extends Acceptor {

    public NodeAcceptor(ServerSocket serverSocket) {
        super(serverSocket);
    }

    @Override
    public Connection handleConnection(Socket clientSocket) {
        // TODO:
        return null;
    }
}
