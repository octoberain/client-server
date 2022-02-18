import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class MyThread implements Runnable {
    private String phrase;
    private int[] randomSkierId;
    private int[] randomTime;
    private int iterations;
    private String baseURL;
    private RequestCounter counter;
    private long startTime;
    private int successCount;
    private int failureCount;

    private CountDownLatch prevLatch;
    private CountDownLatch currLatch;
    private CountDownLatch finalLatch;

    private final static String SEASON_ID = "2022";
    private final static String DAY_ID = "136";
    private final static int RESORT_ID = 12345;
    private final int RETRY = 5;

    private SkiersApi apiInstance = new SkiersApi();

    public MyThread(String phrase, int[] randomSkierId, int[] randomTime, int iterations,
                    CountDownLatch prevLatch, CountDownLatch currLatch, CountDownLatch finalLatch, RequestCounter counter) {
        this.phrase = phrase;
        this.randomSkierId = randomSkierId;
        this.randomTime = randomTime;
        this.iterations = iterations;
        this.baseURL = Utils.loadURL();
        this.counter = counter;
        this.prevLatch = prevLatch;
        this.currLatch = currLatch;
        this.finalLatch = finalLatch;
        this.apiInstance.getApiClient().setBasePath(this.baseURL);

    }

    public void run() {

        try {
            prevLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < iterations; i++) {
            // Prepare Request BODY
            LiftRide body = new LiftRide();
            body.setTime(ThreadLocalRandom.current().nextInt(randomTime[0], randomTime[1]) + 1);
            body.setLiftID(ThreadLocalRandom.current().nextInt(SkiClient.numLifts) + 1);
            body.setWaitTime(ThreadLocalRandom.current().nextInt(10) + 1);

            // set start time for current request
            this.startTime = System.currentTimeMillis();
            int skierId = ThreadLocalRandom.current().nextInt(randomSkierId[0], randomSkierId[1]);

            // For each request try multiple times
            for (int k = 0; k < RETRY; k++) {
                try {
                    ApiResponse<Void> response = apiInstance.writeNewLiftRideWithHttpInfo(body, RESORT_ID, SEASON_ID, DAY_ID, skierId);
                    System.out.println(this.phrase + " " + Thread.currentThread() + " status: " + response.getStatusCode());
                    // count success
                    successCount++;
                    break;
                } catch (ApiException e) {
                    if (k == RETRY) {
                        failureCount++;
                    }
                    e.printStackTrace();
                }
            }

        }
        if (currLatch != null)
            currLatch.countDown();
        finalLatch.countDown();

        counter.countSuccess(successCount);
        counter.countFailure(failureCount);
    }
}
