package stress_testing;

import java.util.Random;
import java.util.ArrayList;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import logger.LogSetup;

import app_kvClient.*;
import app_kvServer.*;

public class StressTest implements Runnable {

    private static String INITIALHOSTNAME;
    private static int INITIALPORT;

    private static class StressTestResults {

        // Latencies
        public double avg_put_RTT = 0;
        public double avg_get_RTT = 0;

        // These two will converge to throughput
        public double total_requests;
        public double total_time_elapsed;
    }
    private double _putRTTTotal = 0;
    private double _getRTTTotal = 0;

    public StressTestResults _results = null;

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

    public static void printResultsHeader() {
        System.out.println("numClients,throughput");
    }

    public static void printResults(
        StressTestResults results, 
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
        System.out.println(numClients + "," + putRatioString + "," + results.avg_put_RTT + "," + results.avg_get_RTT);
    }

    private int numRequests;
    private int putRatio;

    public StressTest(
        int numRequests,
        int putRatio) {
        this.numRequests = numRequests;
        this.putRatio = putRatio;
    }

    /**
     * Put ratio types: 1=4:1
     *                  2=1:1
     *                  3=1:4
     */

    @Override
    public void run() {

        IKVClient kvClient = null; // TODO

        StressTestResults results = new StressTestResults();

        double num_puts = 0;
        if (putRatio == 1) {
            num_puts = 0.8 * numRequests;
        } else if (putRatio == 2) {
            num_puts = 0.5 * numRequests;
        } else {
            num_puts = 0.2 * numRequests;
        }

        double num_gets = numRequests - num_puts;

        try {
            // Initialize & connect client(s)
            kvClient = new KVClient();
            kvClient.newConnection(
                "INTIAIL", 
                INITIALHOSTNAME, 
                INITIALPORT
            );

            // Perform put put put get, etc.
            // Each GET must get a random key that has previously been PUT.
            for (int reqNum = 0; reqNum < numRequests; reqNum++) {
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
            } catch (Exception e) {
                System.out.println("Failed to teardown client");
            }
        }

        _results = results;
    }

    public static void main (String[] args) {
        if (args.length != 2) {
            System.out.println("usage: <initial server host> <initial server port>");
            return;
        }

        INITIALHOSTNAME = args[0];
        INITIALPORT = Integer.parseInt(args[1]);

        Logger logger = Logger.getRootLogger();
        logger.info("running stress test");
        StressTest.printResultsHeader();

        int[] numClients = {1, 10, 100};

        for (int numClient : numClients) {


            Thread[] threads = new Thread[numClient];
            StressTest[] stressTests = new StressTest[numClient];

            long startTimeNano = System.nanoTime();

            for (int i = 0; i < numClient; i++) {
                stressTests[i] = new StressTest(NUM_REQUESTS / numClient, 1);
                threads[i] = new Thread(stressTests[i]);
                threads[i].run();
            }

            for (int i = 0; i < numClient; i++) {
                try {
                    threads[i].join();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }

            long endTimeNano = System.nanoTime();

            double throughput = NUM_REQUESTS * 1.0 / (endTimeNano - startTimeNano);

            System.out.println(numClient + "," + throughput);
        }
        System.out.println("all tests completed");
        return;
    }


}