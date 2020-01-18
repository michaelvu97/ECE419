package app_kvClient;

import client.KVCommInterface;

public class KVClient implements IKVClient {
    @Override
    public void newConnection(String hostname, int port){
        // TODO Auto-generated method stub
    }

    @Override
    public KVCommInterface getStore(){
        
        // This will return a new KVStore

        // TODO Auto-generated method stub
        return null;
    }

    //passes put request to kvstore object
    public void put(String key, String value){
        //clientStore.get(key,value);
        //add a lil print message for testing
    }

    //passes get request to kvstore object
    public void get(String key){
        //clientStore.put(key,value);
        //add a lil print message for testing
    }

    //passes disconnect request to kvstore object
    public void closeConnection(){
        //clientStore.disconnect();
        //add a lil print message for testing
    }
}
