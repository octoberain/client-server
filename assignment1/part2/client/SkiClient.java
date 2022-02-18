import java.util.Map;
import java.util.concurrent.*;

public class SkiClient {
    static DataTool dataTool;
    static int numRuns;
    static int numThreads;
    static int numLifts;
    static int numSkiers;
    private long totalWallTime;
    private int totalRequests;
    private RequestCounter counter;
    private static final String WARMUP = "WarmUp";
    private static final String PEAK = "Peak";
    private static final String COOLDOWN = "CoolDown";


    public SkiClient() {
        loadParams();
        dataTool = new DataTool();
        counter = new RequestCounter();
    }

    /**
     * Launch multi-threads to make POST request
     */
    public void launch() {
        CountDownLatch startupCDL = new CountDownLatch(0);
        CountDownLatch peakCDL = new CountDownLatch((int) (numThreads / 4 * 0.2));
        CountDownLatch coolDownCDL = new CountDownLatch((int) (numThreads * 0.2));
        CountDownLatch finalCDL = new CountDownLatch(numThreads / 4 + numThreads + numThreads / 10);

        // ------ start counting -------
        long startWall = System.currentTimeMillis();
        Phase phase1 = new Phase(WARMUP, numThreads / 4, 1, 90, 0.2, startupCDL, peakCDL, finalCDL, counter);
        Phase phase2 = new Phase(PEAK, numThreads, 91, 360, 0.6, peakCDL, coolDownCDL, finalCDL, counter);
        Phase phase3 = new Phase(COOLDOWN, numThreads / 10, 361, 420, 0.1, coolDownCDL, null, finalCDL, counter);

        phase1.run();
        phase2.run();
        phase3.run();
        // ------ finish counting -------

        try {

            finalCDL.await(); // Wait for all Threads to finish
            long afterWall = System.currentTimeMillis();

            totalWallTime = afterWall - startWall;
            dataTool.wallTime = totalWallTime;
            totalRequests = counter.getSuccessCount() + counter.getFailureCount();
            this.addRecords(counter.getRecords());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            dataTool.close();
        }

    }

    private void loadParams() {
        Map<String, Integer> params = Utils.loadParams();
        numRuns = params.get(Utils.NUMRUNS);
        numThreads = params.get(Utils.NUMTHREADS);
        numLifts = params.get(Utils.NUMLIFTS);
        numSkiers = params.get(Utils.NUMSKIERS);
    }

    private void addRecords(BlockingDeque<Record> records) {
        for (Record r : records) {
            dataTool.writeToCSV(r);
        }
    }

    public void printThreadsStats() {
        System.out.println(String.format("\nTotal Success: %d", counter.getSuccessCount()));
        System.out.println(String.format("Total Failure: %d", counter.getFailureCount()));

        System.out.println("\n------ SYSTEM RUNNING STATISTICS ------");
        Utils.printMsg("Wall time", totalWallTime, "milliseconds");
        Utils.printMsg("Throughput", Math.round(totalRequests * 1000 / totalWallTime), "per second");
    }

    public void printLatencyStats() {
        dataTool.printStats();
    }

}
