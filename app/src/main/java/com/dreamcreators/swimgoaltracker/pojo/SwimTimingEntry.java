package com.dreamcreators.swimgoaltracker.pojo;

public class SwimTimingEntry {
    private long id;
    private long timeMs;
    private long createdAtMs;
    private String date;
    private long flyMs;
    private long backMs;
    private long breastMs;
    private long freeMs;

    public SwimTimingEntry(long id, long timeMs, long createdAtMs, String date) {
        this.id = id;
        this.timeMs = timeMs;
        this.createdAtMs = createdAtMs;
        this.date = date;
    }

    public void setSplits(long flyMs, long backMs, long breastMs, long freeMs) {
        this.flyMs = flyMs;
        this.backMs = backMs;
        this.breastMs = breastMs;
        this.freeMs = freeMs;
    }

    public long getId() { return id; }
    public long getTimeMs() { return timeMs; }
    public long getCreatedAtMs() { return createdAtMs; }
    public String getDate() { return date; }
    public long getFlyMs() { return flyMs; }
    public long getBackMs() { return backMs; }
    public long getBreastMs() { return breastMs; }
    public long getFreeMs() { return freeMs; }
}
