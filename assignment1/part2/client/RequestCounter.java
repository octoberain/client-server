import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class RequestCounter {

    private int successCount;
    private int failureCount;
    private BlockingDeque<Record> records;

    public RequestCounter() {
        this.successCount = 0;
        this.failureCount = 0;
        this.records = new LinkedBlockingDeque<>();
    }

    synchronized public void countSuccess(int count) {

        successCount += count;
    }

    synchronized public void countFailure(int count) {

        failureCount += count;
    }

    public void addRecords(List<Record> records) {
        this.records.addAll(records);
    }

    public BlockingDeque<Record> getRecords() {
        return records;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

}
