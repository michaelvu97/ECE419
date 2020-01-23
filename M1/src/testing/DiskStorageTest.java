package testing;

import org.junit.Test;
import storage.DiskStorage;

import junit.framework.TestCase;

public class DiskStorageTest extends TestCase {
		
	private DiskStorage diskStorage; 
		
	public void setUp() {
		diskStorage = new DiskStorage("TEST_STORAGE");
	}

	public void tearDown() {
		diskStorage.clear();
	}

	@Test
	public void testWriteNewEntries() {
		Exception ex = null;
		int write1 = 0, write2 = 0, write3 = 0;
		String key1 = "Key1";
		String value1 = "Value1";
		String key2 = "Key2";
		String value2 = "Value2";
		String key3 = "Key3";
		String value3 = "Value3";

		try {
			write1 = diskStorage.put(key1, value1); 
			write2 = diskStorage.put(key2, value2); 
			write3 = diskStorage.put(key3, value3); 
		} catch (Exception e) {
			ex = e;
		}
		
		assertTrue(ex == null);
		assertTrue(write1 == 1);
		assertTrue(write2 == 1);
		assertTrue(write3 == 1);
	}

	@Test
	public void testReplaceEntryValue() {
		Exception ex = null;
		int write = 0;
		int replace = 0;
		String key1 = "Key4";
		String value1 = "Value4";
		String newValue1 = "NEWValue4";

		try {
			write = diskStorage.put(key1, value1); 
			replace = diskStorage.put(key1, newValue1); 
		} catch (Exception e) {
			ex = e;
		}
		
		assertTrue(ex == null);
		assertTrue(write == 1);
		assertTrue(write == 1);
	}

	@Test 
	public void testReadEntry() {
		int write = 0;
		String read = null;
		Exception ex = null;
		String key = "KeyToRead";
		String value = "Value to read!";

		try {
			write = diskStorage.put(key, value);
			read = diskStorage.get(key);
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex == null);
		assertEquals(value, read);
	}

	@Test 
	public void testReadNonExistantEntry() {
		String read = null;
		Exception ex = null;
		String key = "NonExistantKey";

		try {
			read = diskStorage.get(key);
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex == null);
		assertEquals(null, read);
	}

	@Test 
	public void testDeleteEntry() {
		int write = 0;
		Exception ex = null;
		String deleteKey = "KeyToDelete";
		String deleteValue = "Value to delete!";

		try {
			write = diskStorage.put(deleteKey, deleteValue);
			diskStorage.delete(deleteKey);
		} catch (Exception e) {
			ex = e;
		}

		assertTrue(ex == null);
	}

	@Test
	public void testWriteAndDeleteNewEntires() {
		Exception ex = null;
		int writeA = 0, writeB = 0, writeC = 0;
		String keyA = "KeyA";
		String valueA = "ValueA";
		String keyB = "KeyB";
		String valueB = "ValueB";
		String keyC = "KeyC";
		String valueC = "ValueC";

		try {
			writeA = diskStorage.put(keyA, valueA); 
			writeB = diskStorage.put(keyB, valueB); 
			diskStorage.delete(keyB);
			writeC = diskStorage.put(keyC, valueC); 
		} catch (Exception e) {
			ex = e;
		}
		
		assertTrue(ex == null);
		assertTrue(writeA == 1);
		assertTrue(writeB == 1);
		assertTrue(writeC == 1);
	}

	// @Test 
	// public void testDeleteFile() {
	// 	diskStorage.deleteFile();
	// }
}