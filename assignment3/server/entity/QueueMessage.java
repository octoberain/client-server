package entity;

/**
 * Class is used to store skier lift data for each POST request
 */
public class QueueMessage {

    private String resortId;
    private String seasonId;
    private String dayId;
    private String skierId;
    private long time;
    private long liftId;
    private long waitTime;

    public QueueMessage(String resortId, String seasonId, String dayId, String skierId) {
        this.resortId = resortId;
        this.seasonId = seasonId;
        this.dayId = dayId;
        this.skierId = skierId;
    }

    public String getResortId() {
        return resortId;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public String getDayId() {
        return dayId;
    }

    public String getSkierId() {
        return skierId;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setLiftId(long liftId) {
        this.liftId = liftId;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }
}
