package entity;

public class Resort {

    private int resortID;
    private String resortName;

    public Resort(int resortID, String resortName) {
        this.resortID = resortID;
        this.resortName = resortName;
    }

    public int getResortID() {
        return resortID;
    }

    public void setResortID(int resortID) {
        this.resortID = resortID;
    }

    public String getResortName() {
        return resortName;
    }

    public void setResortName(String resortName) {
        this.resortName = resortName;
    }
}
