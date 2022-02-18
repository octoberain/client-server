import java.util.concurrent.CountDownLatch;

public class Phase {
    private String name;
    private int skierIdRange;
    private int rideStartTime;
    private int rideEndTime;
    private int numThreads;
    private int iterations;
    private RequestCounter counter;

    private CountDownLatch prevLatch;
    private CountDownLatch currLatch;
    private CountDownLatch finalLatch;

    public Phase(String name, int numThreads, int rideStartTime, int rideEndTime,
                 double runCapacity, CountDownLatch prevLatch, CountDownLatch currLatch, CountDownLatch finalLatch, RequestCounter counter) {

        this.name = name;
        this.numThreads = numThreads;
        this.skierIdRange = SkiClient.numSkiers / numThreads;
        this.iterations = (int) (SkiClient.numRuns * runCapacity * skierIdRange);
        this.rideStartTime = rideStartTime;
        this.rideEndTime = rideEndTime;
        this.counter = counter;
        this.prevLatch = prevLatch;
        this.currLatch = currLatch;
        this.finalLatch = finalLatch;

    }

    public void run() {
        for (int i = 0; i < numThreads; i++) {
            MyThread thread = new MyThread(
                    name,
                    new int[]{skierIdRange * i, skierIdRange * (i + 1)},
                    new int[]{rideStartTime, rideEndTime},
                    iterations, prevLatch, currLatch, finalLatch, counter);
            new Thread(thread).start();
        }
    }

}
