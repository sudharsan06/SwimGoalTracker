package com.dreamcreators.swimgoaltracker.pojo;

public class TrackerPojo {
    public String date;
    public String nutrition;
    public String swimActivity;
    public String freeTime;
    public String flyTime;
    public String breastTime;
    public String backTime;
    public String imTime;
    public long freeMs, flyMs, breastMs, backMs, imMs;

    public TrackerPojo(String date, String nutrition, String swimActivity) {
        this.date = date;
        this.nutrition = nutrition;
        this.swimActivity = swimActivity;
    }

    public TrackerPojo(String date, String freeTime, String flyTime, String breastTime, String backTime, String imTime,
                       long freeMs, long flyMs, long breastMs, long backMs, long imMs) {
        this.date = date;
        this.freeTime = freeTime;
        this.flyTime = flyTime;
        this.breastTime = breastTime;
        this.backTime = backTime;
        this.imTime = imTime;
        this.freeMs = freeMs;
        this.flyMs = flyMs;
        this.breastMs = breastMs;
        this.backMs = backMs;
        this.imMs = imMs;
    }
}
