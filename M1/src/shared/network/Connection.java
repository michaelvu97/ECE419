package shared.network;

import java.io.IOException;
import java.net.Socket;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import shared.comms.*;

public abstract class Connection implements Runnable {

    protected Socket socket;
    protected Acceptor parentAcceptor;

    protected ICommChannel commChannel;

    protected boolean isOpen = true;
    protected boolean exit = false;

    private static Logger _logger = Logger.getRootLogger();

    protected Connection(Socket socket, Acceptor acceptor) {
        if (socket == null)
            throw new IllegalArgumentException("socket is null");

        this.socket = socket;
        this.parentAcceptor = acceptor;

        try {
            this.commChannel = new CommChannel(socket);
        } catch (IOException ioe) {
            this.isOpen = false;
            this.socket = null;
            this.commChannel = null;
            _logger.error("Failed to establish connection comm channel", ioe);
        }
    }

    public void stop() {
        _logger.info("Stopping connection thread.");

        this.isOpen = false;
        parentAcceptor.alertClose(this);

        int millisSlept = 0;
        int sleepTimeoutMillis = 5000;

        while (socket != null) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                _logger.warn("Thread stop sleep error", e);
                break;
            }
            millisSlept += 100;
            if (millisSlept >= sleepTimeoutMillis) {
                _logger.error("Thread stop timed out");
                break;
            }
        }

        // At this point, the socket is either closed or closing it has timed
        // out.
        return;
    }

    public void kill() {
        exit = true;
    }

    public abstract void work() throws Exception;

    @Override
    public void run() {
        _logger.info("Connection thread started");
        try {
            while(isOpen && !exit) { 
                work();
            }
        } catch (Exception e) {
            _logger.error("Error in connection work", e);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                    parentAcceptor.alertClose(this);
                    _logger.info("Client disconnected, socket closed");
                }
            } catch (IOException ioe) {
                _logger.error("Error! Unable to tear down connection!", ioe);
            } finally {
                socket = null;
            }
        }
    }
}