package entity;

import lombok.Data;

@Data
public class LiftRideRequestBody {

    private long time;
    private long liftID;
    private long waitTime;

}
