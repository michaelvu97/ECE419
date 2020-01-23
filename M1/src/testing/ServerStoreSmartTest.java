package testing;

import client.KVStore;

import org.junit.Test;

import client.KVStore;
import junit.framework.TestCase;
import shared.messages.KVMessage;
import shared.messages.KVMessage.StatusType;

import server.*;
import app_kvServer.*;
import cache.*;
import storage.*;

public class ServerStoreSmartTest extends TestCase {

    IServerStore serverStore = null;

    public void setUp() {
        ICache cache = new Cache(1000, IKVServer.CacheStrategy.FIFO);
        IDiskStorage diskStorage = new DiskStorage("SERVER_STORE_TEST");
        serverStore = new ServerStoreSmart(cache, diskStorage);
    }

    public void tearDown() {
        serverStore.clearStorage();
    }

    @Test
    public void testBasic() {
        assertNull(serverStore.get("nonexistant"));
        assertTrue(serverStore.put("a", "b") == IServerStore.PutResult.INSERTED);
        assertTrue(serverStore.put("a", "c") == IServerStore.PutResult.UPDATED);
        assertTrue(serverStore.put("a", "d") == IServerStore.PutResult.UPDATED);
        assertTrue(serverStore.get("a").equals("d"));
        assertTrue(serverStore.delete("a"));
        assertNull(serverStore.get("a"));
        assertTrue(serverStore.put("a", "b") == IServerStore.PutResult.INSERTED);
    }
}

