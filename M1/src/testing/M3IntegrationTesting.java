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

	private KVStore getKVS() {
		KVStore kvs = new KVStore(new ServerInfo("unknown?!", "localhost", 50000));
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

	private String get(KVStore kvs, String key) {
		try {
			KVMessage result = kvs.get(key);
			assertTrue(result.getStatus() == KVMessage.StatusType.GET_SUCCESS);
			return result.getValue();
		} catch (Exception e){
			e.printStackTrace();
			assertTrue(false);
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
		 * Create an ecs server, add 3 kv servers, perform simple I/O, make sure it works.
		 */
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(3, "FIFO", 10);

		KVStore kvs = getKVS();
		put(kvs, "1", "1");
		put(kvs, "2", "2");
		put(kvs, "3", "3");
		put(kvs, "4", "4");

		assertTrue(get(kvs, "1").equals("1"));
		assertTrue(get(kvs, "2").equals("2"));
		assertTrue(get(kvs, "3").equals("3"));
		assertTrue(get(kvs, "4").equals("4"));

		ecsClient.shutdown();	
	}

	@Test
	public void testRemovalTransferBasic() {
		/**
		 * Creates 4 zk servers, adds 10 entries, and confirms that the entries still exist after removing a server
		 */
		IECSClient ecsClient = getECS();
		ecsClient.addNodes(4, "FIFO", 10);

		KVStore kvs = getKVS();

		String[] entries = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

		for (String key : entries) {
			put(kvs, key, key);
		}

		// Remove the server
		List<String> serversToRemove = new ArrayList<String>();
		serversToRemove.add("server1");
		ecsClient.removeNodes(serversToRemove); // Will fail if they're renamed, so I guess don't do that.

		for (String key : entries) {
			assertTrue(get(kvs, key).equals(key));
		}		

		ecsClient.shutdown();
	}
}