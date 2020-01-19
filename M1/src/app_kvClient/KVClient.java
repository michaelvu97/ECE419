package app_kvClient;

import client.KVCommInterface;
import client.ClientSocketListener;
import client.KVStore;

public class KVClient implements IKVClient {

	private KVStore clientStore = null;

    @Override
    public void newConnection (String hostname, int port) throws Exception {
		clientStore = new KVStore(hostname, port);
		clientStore.connect();
	}

    @Override
    public KVCommInterface getStore(){
        if (clientStore != null) {
        	return clientStore;
        }
        return null;
    }

    public void put(String key, String value){
        clientStore.put(key, value);
        //add a lil print message for testing
    }

    public void get(String key){
        clientStore.get(key);
        //add a lil print message for testing
    }

    public void closeConnection(){
        clientStore.disconnect();
        //add a lil print message for testing
    }

    public void addListener(ClientSocketListener listener){
    	clientStore.addListener(listener);
    }
}
