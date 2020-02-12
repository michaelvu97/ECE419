package app_kvClient;

import java.io.IOException;

import client.KVCommInterface;
import client.ClientSocketListener;
import client.KVStore;

import shared.metadata.*;

public class KVClient implements IKVClient {

	private KVStore clientStore = null;

    @Override
    public void newConnection (String serverName, String hostname, int port) 
            throws IOException {
        // TODO: check that the connection doesn't already exist?
		clientStore = new KVStore(new ServerInfo(serverName, hostname, port));
		clientStore.connect();
	}

    @Override
    public KVCommInterface getStore() throws IllegalStateException {
        ValidateConnectionEstablished();
        return clientStore;
    }

    public void put(String key, String value) throws Exception, IllegalStateException {
        ValidateConnectionEstablished();
        clientStore.put(key, value);
    }

    public void get(String key) throws Exception, IllegalStateException {
        ValidateConnectionEstablished();
        clientStore.get(key);
    }

    public void disconnect() throws IllegalStateException {
        ValidateConnectionEstablished();
        clientStore.disconnect();
    }

    // public void addListener(ClientSocketListener listener) throws IllegalStateException {
    //     ValidateConnectionEstablished();
    // 	clientStore.addListener(listener);
    // }

    private void ValidateConnectionEstablished() throws IllegalStateException {
        if (clientStore == null) {
            throw new IllegalStateException(
                "Connection must be established before accessing store"
            );
        }
    }
}
