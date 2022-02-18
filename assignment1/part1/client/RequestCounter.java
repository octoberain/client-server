public class RequestCounter {

    private int successCount;
    private int failureCount;

    public RequestCounter() {
        this.successCount = 0;
        this.failureCount = 0;
    }

    synchronized public void countSuccess(int count) {

        successCount += count;
    }

    synchronized public void countFailure(int count) {

        failureCount += count;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

}
