package testing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import junit.framework.TestCase;

import client.*;
import shared.metadata.*;
import shared.messages.*;
import app_kvECS.*;

public class M3IntegrationTesting extends TestCase {

	private static String DEFAULT_ECS_CONFIG = "./src/app_kvECS/ecs.config";

	private static String[] A_BUNCH_OF_KEYS;

	static {
		int N_keys = 20;
		A_BUNCH_OF_KEYS = new String[N_keys];
		for (int i = 0; i < N_keys; i++) {
			A_BUNCH_OF_KEYS[i] = Integer.toString(i);
		}
	}

	private IECSClient getECS() {
		wipeZk();
		IECSClient ecs = new ECSClient(DEFAULT_ECS_CONFIG, null);
		try {
			ecs.start();
			return ecs;
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		return null;
	}

	private KVStoreTest getKVS() {
		KVStoreTest kvs = new KVStoreTest(new ServerInfo("unknown?!", "localhost", 50000));
		try {
			kvs.connect();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		return kvs;
	}

	private void wipeZk() {
		runScript("./src/testing/integration_wipe_zk.sh");
	}

	private void put(KVStore kvs, String key, String value) {
		try {
			KVMessage res = kvs.put(key, value);
			assertTrue(res.getStatus() == KVMessage.StatusType.PUT_SUCCESS ||
						res.getStatus() == KVMessage.StatusType.PUT_UPDATE);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	private String get(KVStoreTest kvs, String key) {
		return getFromReplica(kvs, key, 0);
	}

	private String getFromReplica(KVStoreTest kvs, String key, int replicaNum) {
		try {
			KVMessage result = kvs.get(key, replicaNum);
			assertTrue(result != null);
			assertTrue(result.getStatus() == KVMessage.StatusType.GET_SUCCESS);
			return result.getValue();
		} catch (Exception e) { // is catch exception the correct one
			e.printStackTrace();
			assertTrue(false); // lol
		} 
		return null;
	}

	private void runScript(String name) {
		String cmd[] = {
			name
		};

		try {
			Process proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Override
	public void setUp() {
		runScript("./src/testing/integration_startup.sh");
	}

	@Override
	public void tearDown() {
		runScript("./src/testing/integration_teardown.sh");
	}

	@Test
	public void testBasic() {
		/**
		 * Create an ecs server, add 3 kv servers, perform simple I/O, make 
		 * sure it works.
		 */
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(3, "FIFO", 10);

		KVStoreTest kvs = getKVS();
		put(kvs, "1", "1");
		put(kvs, "2", "2");
		put(kvs, "3", "3");
		put(kvs, "4", "4");

		assertTrue(get(kvs, "1").equals("1"));
		assertTrue(get(kvs, "2").equals("2"));
		assertTrue(get(kvs, "3").equals("3"));
		assertTrue(get(kvs, "4").equals("4"));

		kvs.disconnect();
		assertTrue(ecsClient.shutdown());	
	}

	@Test
	public void testRemovalTransferBasic() {
		/**
		 * Creates 4 zk servers, adds 10 entries, and confirms that the entries
		 * still exist after removing a server
		 */
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(4, "FIFO", 10);

		KVStoreTest kvs = getKVS();

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key);
		}

		// Remove the server
		List<String> serversToRemove = new ArrayList<String>();
		serversToRemove.add("server_1");
		
		// Will fail if they're renamed, so I guess don't do that.
		ecsClient.removeNodes(serversToRemove); 
		
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}		

		serversToRemove.clear();
		serversToRemove.add("server_2");
		serversToRemove.add("server_3");

		ecsClient.removeNodes(serversToRemove);

		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}		

		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}

	@Test
	public void testAddingTransferBasic() {
		/**
		 * Creates 4 zk servers, adds 10 entries, and confirms that the entries
		 * still exist after  adding a 5th server.
		 */
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(4, "FIFO", 10);

		KVStoreTest kvs = getKVS();

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key);
		}

		ecsClient.addNodes(1, "FIFO", 10);

		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}

		ecsClient.addNodes(2, "FIFO", 10);

		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}

		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}

	@Test
	public void testSendToKilledServerA() {
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(4, "LRU", 10);

		KVStoreTest kvs = getKVS();

		ecsClient.killNode("server_3");

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key);
			assertTrue(get(kvs, key).equals(key));
		}

		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}

	@Test
	public void testSendToKilledServerB() {
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(4, "LRU", 10);

		KVStoreTest kvs = getKVS();

		ecsClient.removeNode("server_2");
		ecsClient.killNode("server_3");

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key);
			assertTrue(get(kvs, key).equals(key));
		}

		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}

	@Test
	public void testSendToKilledServerC() {
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(4, "LRU", 10);

		KVStoreTest kvs = getKVS();

		ecsClient.killNode("server_4");

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key);
			assertTrue(get(kvs, key).equals(key));
		}

		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}

	@Test
	public void testSendToKilledInitialServer() {
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(4, "LFU", 10);

		KVStoreTest kvs = getKVS();

		// Experimentally verified to go to server_1
		put(kvs, "4", "testSendToKilledInitialServer");

		// kvs is now connected to 1, and has metadataset.
		ecsClient.killNode("server_1");

		assertTrue(get(kvs, "4").equals("testSendToKilledInitialServer"));

		try {
			Thread.sleep(8000);
		} catch (Exception e) {

		}

		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}

	@Test
	public void testAddingAndRemovingTransferBasic() {
		/**
		 * Combination of previous 2 tests (addition and removal of servers):
		 * Creates 4 zk servers, adds 10 entries, confirms that the entries
		 * still exist after adding a 5th server. Removes 2 servers, confirms
		 * that entries still exist after removal.
		 */
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(4, "FIFO", 10);

		KVStoreTest kvs = getKVS();

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key);
		}

		// assert that initial 4 zk servers work.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}

		// add 5th server.
		ecsClient.addNodes(1, "FIFO", 10);

		// assert that adding 5th server works.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}

		// remove 2 servers.
		List<String> serversToRemove = new ArrayList<String>();
		serversToRemove.add("server_1");
		serversToRemove.add("server_2");
		
		ecsClient.removeNodes(serversToRemove); 

		// assert that removing 2 servers works.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}

		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}

	// TODO: run test on working machine.
	@Test
	public void testReplicationWithThreeServers() {
		/**
		 * Replication Case: 3 servers.
		 * Creates 3 servers. Tests that all 3 servers contain the same information,
		 * as they are all replicas of the primary, and should hold the same info.
		 */
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(3, "FIFO", 10);

		KVStoreTest kvs = getKVS();

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key);
		}

		// assert that the 3 zk servers work.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}

		// check that primarys have correct values (redundant but ¯\_(ツ)_/¯).
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(getFromReplica(kvs, key, 0).equals(key));
		}

		// check that replica 1s have correct values.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(getFromReplica(kvs, key, 1).equals(key));
		}

		// check that replica 2s have correct values.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(getFromReplica(kvs, key, 2).equals(key));
		}

		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}

	// TODO: run test on working machine.
	// need confirmation that the A_BUNCH_OF_KEYS will distribute among all 5 servers.
	@Test
	public void testReplicationWithFiveServers() {
		/**
		 * Replication Case: 3+ servers.
		 * Creates 5 servers. Tests that all servers have 2 replicas that
		 * contain the correct information. 
		 */
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(5, "FIFO", 10);

		KVStoreTest kvs = getKVS();

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key);
		}

		// assert that the 5 zk servers work.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}

		// check that primaries have correct values.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(getFromReplica(kvs, key, 0).equals(key));
		}

		// check that replica 1s have correct values.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(getFromReplica(kvs, key, 1).equals(key));
		}

		// check that replica 2s have correct values.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(getFromReplica(kvs, key, 2).equals(key));
		}
		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}

	// TODO: run test on working machine.
	@Test
	public void testReplicationWithTwoServers() {
		/**
		 * Replication Case: 2 servers.
		 * Creates 2 servers. Tests that all each server has 1 replica.
		 */
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(2, "FIFO", 10);

		KVStoreTest kvs = getKVS();

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key);
		}

		// assert that the 2 zk servers work.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key));
		}

		// check that primaries have correct values.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(getFromReplica(kvs, key, 0).equals(key));
		}

		// check that replica 1s have correct values.
		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(getFromReplica(kvs, key, 1).equals(key));
		}

		// there are no replica 2s in this scenario. 
		kvs.disconnect();
		assertTrue(ecsClient.shutdown());
	}


	@Test
	public void testPersistency() {
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(3, "FIFO", 10);

		KVStoreTest kvs = getKVS();

		for (String key : A_BUNCH_OF_KEYS) {
			put(kvs, key, key + "_test_persistency");
		}

		kvs.disconnect();

		assertTrue(ecsClient.shutdown());

		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			// Nothing.
		}

		ecsClient = getECS();
		ecsClient.addNodes(3, "FIFO", 10);

		kvs = getKVS();

		for (String key : A_BUNCH_OF_KEYS) {
			assertTrue(get(kvs, key).equals(key + "_test_persistency"));
		}

		kvs.disconnect();

		assertTrue(ecsClient.shutdown());
	}
}