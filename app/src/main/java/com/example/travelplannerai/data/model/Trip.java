package com.example.travelplannerai.data.model;

public class Trip {
    private int id;
    private String destination; // used for upcoming trips
    private String dates;       // used for upcoming trips
    private String title;       // used for recent activity
    private String subtitle;    // used for recent activity
    private String date;        // used for recent activity
    private String imageUrl;
    private boolean isActivity; // helper to distinguish

    // Constructor for upcoming trips
    public Trip(int id, String destination, String dates, String imageUrl) {
        this.id = id;
        this.destination = destination;
        this.dates = dates;
        this.imageUrl = imageUrl;
        this.isActivity = false;
    }

    // Constructor for recent activity
    public Trip(int id, String title, String subtitle, String date, String imageUrl, boolean isActivity) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.date = date;
        this.imageUrl = imageUrl;
        this.isActivity = isActivity;
    }

    public int getId() { return id; }
    public String getDestination() { return destination; }
    public String getDates() { return dates; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getDate() { return date; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActivity() { return isActivity; }
}
