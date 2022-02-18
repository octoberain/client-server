import java.util.Objects;

/**
 * Class used for storing HTTP request info
 */
public class Record {

    private long startTime;
    private long latency;
    Utils.RequestType requestType;
    int responseCode;

    public Record(long startTime, long latency, Utils.RequestType requestType, int responseCode) {
        this.startTime = startTime;
        this.latency = latency;
        this.requestType = requestType;
        this.responseCode = responseCode;
    }

    public long getLatency() {
        return latency;
    }

    public String getStartTime() {
        return Utils.convertTime(this.startTime);
    }

    public Utils.RequestType getRequestType() {
        return requestType;
    }

    public int getResponseCode() {
        return responseCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return startTime == record.startTime &&
                latency == record.latency &&
                responseCode == record.responseCode &&
                requestType.equals(record.requestType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, latency, requestType, responseCode);
    }

    @Override
    public String toString() {
        return "Record{" +
                "startTime=" + startTime +
                ", latency=" + latency +
                ", requestType=" + requestType +
                ", responseCode=" + responseCode +
                '}';
    }
}
