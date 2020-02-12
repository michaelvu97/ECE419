package stress_testing;

import java.util.Random;
import java.util.ArrayList;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import logger.LogSetup;

import app_kvClient.*;
import app_kvServer.*;

public class StressTest {

    private class StressTestResults {

        // Latencies
        public double avg_put_RTT = 0;
        public double avg_get_RTT = 0;

        // These two will converge to throughput
        public double total_requests;
        public double total_time_elapsed;
    }
    private double _putRTTTotal = 0;
    private double _getRTTTotal = 0;

    private static int NUM_REQUESTS = 10000;

    private static Random random = new Random();

    private static String GetNewRandomKey() {
        // Will return a 19-length string that doesn't have whitespace
        String s = "";
        for (int i = 0; i < 18; i++) {
            s = s + ((char) (((char)random.nextInt(26)) + 'a'));
        }
        return s;
    }

    private ArrayList<String> _insertedKeys = new ArrayList<String>();

    private String GetRandomInsertedKey() {
        // Will return a random item from _insertedKeys
        return _insertedKeys.get(random.nextInt(_insertedKeys.size()));
    }

    private String GetPutRequestKey() {
        // 50/50 chance of gettig a new key, or an old key (unless this the old
        //  key list is empty)
        if (_insertedKeys.size() == 0 || random.nextInt(2) == 0) {
            String key = GetNewRandomKey();
            _insertedKeys.add(key);
            return key;
        }

        return GetRandomInsertedKey();
    }

    private static void printResultsHeader() {
        System.out.println("cacheSize,cacheStrategy,numClients,putRatio,put_rtt_nanos,get_rtt_nanos");
    }

    private static void printResults(
        StressTestResults results, 
        int cacheSize, 
        String cacheStrategy, 
        int numClients,
        int putRatio) {

        String putRatioString = "";
        if (putRatio == 1) {
            putRatioString = "80%";
        } else if (putRatio == 2) {
            putRatioString = "50%";
        } else {
            putRatioString = "20%";
        }
        System.out.println(cacheSize + "," + cacheStrategy + "," + numClients + "," + putRatioString + "," + results.avg_put_RTT + "," + results.avg_get_RTT);
    }

    /**
     * Put ratio types: 1=4:1
     *                  2=1:1
     *                  3=1:4
     */

    public StressTest(
        String cacheStrategy, 
        int cacheSize, 
        int numClients,
        int putRatio) {

        IKVServer kvServer = null; // TODO
        IKVClient kvClient = null; // TODO

        StressTestResults results = new StressTestResults();

        double num_puts = 0;
        if (putRatio == 1) {
            num_puts = 0.8 * NUM_REQUESTS;
        } else if (putRatio == 2) {
            num_puts = 0.5 * NUM_REQUESTS;
        } else {
            num_puts = 0.2 * NUM_REQUESTS;
        }

        double num_gets = NUM_REQUESTS - num_puts;

        try {
            

            // Initialize server
            kvServer = new KVServer(0, cacheSize, cacheStrategy, "STRESS_TEST_STORAGE");
            kvServer.run();

            // Wipe the server memory
            kvServer.clearStorage();
            kvServer.clearCache();

            // Initialize & connect client(s)
            kvClient = new KVClient();
            kvClient.newConnection("TODO", kvServer.getHostname(), 
                    kvServer.getPort());

            // Perform put put put get, etc.
            // Each GET must get a random key that has previously been PUT.
            for (int reqNum = 0; reqNum < NUM_REQUESTS; reqNum++) {
                boolean isPut = false;
                if (putRatio == 1 && reqNum % 5 != 4) {
                    isPut = true;
                } else if (putRatio == 2 && reqNum % 2 == 0) {
                    isPut = true;
                } else if (putRatio == 3 && reqNum % 5 == 0) {
                    isPut = true;
                }

                String key;

                // Get keys
                if (isPut) {
                    key = GetPutRequestKey();
                } else {
                    key = GetRandomInsertedKey();
                }

                // Mark starting time
                long startTimeNano = System.nanoTime();
                if (isPut) {
                    String value = GetNewRandomKey();
                    
                    // Make put request
                    kvClient.getStore().put(key, value);
                } else {
                    // Make get request
                    kvClient.getStore().get(key);
                }

                // Mark ending time. Add to stats.
                long endTimeNano = System.nanoTime();
                double rTTNano = endTimeNano - startTimeNano;

                if (isPut) {
                    results.avg_put_RTT += (rTTNano / num_puts);
                } else {
                    results.avg_get_RTT += (rTTNano / num_gets);
                }
            }
        } catch (Exception e) {
            System.out.println("BAD THING HAPPEN, test results may be inaccurate");
            System.out.println(e);
        } finally {
            try {
                kvClient.getStore().disconnect();
                kvServer.clearStorage();
                kvServer.kill();
            } catch (Exception e) {
                System.out.println("Failed to teardown server");
            }
        }

        printResults(results, cacheSize, cacheStrategy, numClients, putRatio);
    }

    public static void main (String[] args) {
        Logger logger = Logger.getRootLogger();
        logger.info("running stress test");
        StressTest.printResultsHeader();

        String[] cacheStrategies = {"FIFO", "LRU", "LFU"};
        int[] cacheSizes = {10, 100, 1000};
        int[] putRatios = {1, 2, 3};

        int numClients = 1;

        for (String cacheStrategy : cacheStrategies) {
            for (int cacheSize : cacheSizes) {
                for (int putRatio : putRatios) {
                    new StressTest(cacheStrategy, cacheSize, numClients, putRatio);
                }
            }
        }
        System.out.println("all tests completed");
        return;
    }


}