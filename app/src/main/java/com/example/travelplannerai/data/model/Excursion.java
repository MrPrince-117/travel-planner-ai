package com.example.travelplannerai.data.model;

import com.google.firebase.firestore.DocumentId;

public class Excursion {

    @DocumentId
    private String id;
    private String tripId;
    private String name;
    private String location;
    private String date;
    private String time;

    // Constructor vacío requerido por Firestore
    public Excursion() { }

    public Excursion(String tripId, String name, String location, String date, String time) {
        this.tripId   = tripId;
        this.name     = name;
        this.location = location;
        this.date     = date;
        this.time     = time;
    }

    public String getId()           { return id; }
    public void   setId(String id)  { this.id = id; }

    public String getTripId()               { return tripId; }
    public void   setTripId(String tripId)  { this.tripId = tripId; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public String getLocation()                 { return location; }
    public void   setLocation(String location)  { this.location = location; }

    public String getDate()             { return date; }
    public void   setDate(String date)  { this.date = date; }

    public String getTime()             { return time; }
    public void   setTime(String time)  { this.time = time; }
}