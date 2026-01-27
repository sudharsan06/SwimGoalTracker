package com.dreamcreators.swimgoaltracker;

public class TrackerPojo {
    public String date;
    public String nutrition;
    public String swimActivity;
    public String freeTime;
    public String flyTime;
    public String breastTime;
    public String backTime;

    public TrackerPojo(String date, String nutrition, String swimActivity) {
        this.date = date;
        this.nutrition = nutrition;
        this.swimActivity = swimActivity;
    }

    public TrackerPojo(String date, String freeTime, String flyTime, String breastTime, String backTime) {
        this.date = date;
        this.freeTime = freeTime;
        this.flyTime = flyTime;
        this.breastTime = breastTime;
        this.backTime = backTime;
    }
}
