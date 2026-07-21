package com.dreamcreators.swimgoaltracker.pojo;

import org.json.JSONObject;
import org.json.JSONException;

public class SwimEvent {
    private long id;
    private String title;
    private String date;
    private String description;
    private String location;
    private String time;
    private String strokes;
    private String registrationUrl;
    private boolean published;
    private String publishAt;

    public static SwimEvent fromJson(JSONObject obj) throws JSONException {
        SwimEvent e = new SwimEvent();
        e.id = obj.optLong("id", 0);
        e.title = obj.getString("title");
        e.date = obj.getString("date");
        e.description = obj.optString("description", "");
        e.location = obj.optString("location", "");
        e.time = obj.optString("time", "");
        e.strokes = obj.optString("strokes", "");
        e.registrationUrl = obj.optString("registrationUrl", "");
        e.published = obj.optBoolean("published", true);
        e.publishAt = obj.optString("publishAt", "");
        return e;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getTime() { return time; }
    public String getStrokes() { return strokes; }
    public String getRegistrationUrl() { return registrationUrl; }
    public boolean isPublished() { return published; }
    public String getPublishAt() { return publishAt; }
}
