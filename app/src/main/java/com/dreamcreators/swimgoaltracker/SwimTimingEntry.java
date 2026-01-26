package com.dreamcreators.swimgoaltracker;

public class SwimTimingEntry {
    private long id;
    private long timeMs;
    private long createdAtMs;

    public SwimTimingEntry(long id, long timeMs, long createdAtMs) {
        this.id = id;
        this.timeMs = timeMs;
        this.createdAtMs = createdAtMs;
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
}
