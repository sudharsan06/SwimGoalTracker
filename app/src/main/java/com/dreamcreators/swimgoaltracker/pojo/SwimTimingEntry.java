package com.dreamcreators.swimgoaltracker.pojo;

public class SwimTimingEntry {
    private long id;
    private long timeMs;
    private long createdAtMs;
    private String date;

    public SwimTimingEntry(long id, long timeMs, long createdAtMs, String date) {
        this.id = id;
        this.timeMs = timeMs;
        this.createdAtMs = createdAtMs;
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }

    public String getDate() {
        return date;
    }
}
