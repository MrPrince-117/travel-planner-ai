package com.example.travelplannerai.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Modelo POJO que representa un Viaje.
 */
public class Trip {
    @DocumentId
    private String id;
    private String destination;
    private String dates;
    private String imageUrl;
    private Double budget;
    private String userId;
    private String itinerary; // Itinerario generado por IA — se cachea en Firestore

    @ServerTimestamp
    private Date createdAt;

    // Campos auxiliares para actividad reciente
    private String  title;
    private String  subtitle;
    private String  date;
    private boolean isActivity;

    /** Constructor vacío requerido por Firestore. */
    public Trip() { }

    public Trip(int id, String title, String subtitle, String date,
                String imageUrl, boolean isActivity) {
        this.id         = String.valueOf(id);
        this.title      = title;
        this.subtitle   = subtitle;
        this.date       = date;
        this.imageUrl   = imageUrl;
        this.isActivity = isActivity;
        this.budget     = 0.0;
    }

    public Trip(int id, String destination, String dates, String imageUrl) {
        this.id          = String.valueOf(id);
        this.destination = destination;
        this.dates       = dates;
        this.imageUrl    = imageUrl;
        this.isActivity  = false;
        this.budget      = 0.0;
    }

    public Trip(String destination, String dates, String imageUrl, Double budget) {
        this.destination = destination;
        this.dates       = dates;
        this.imageUrl    = imageUrl;
        this.budget      = budget;
        this.isActivity  = false;
    }

    // ==================== GETTERS Y SETTERS ====================

    public String getId()                       { return id; }
    public void   setId(String id)              { this.id = id; }

    public String getDestination()                          { return destination; }
    public void   setDestination(String destination)        { this.destination = destination; }

    public String getDates()                    { return dates; }
    public void   setDates(String dates)        { this.dates = dates; }

    public String getImageUrl()                 { return imageUrl; }
    public void   setImageUrl(String imageUrl)  { this.imageUrl = imageUrl; }

    public Double getBudget()                   { return budget; }
    public void   setBudget(Double budget)      { this.budget = budget; }

    public String getUserId()                   { return userId; }
    public void   setUserId(String userId)      { this.userId = userId; }

    /** Itinerario cacheado generado por IA. Null si aún no se ha generado. */
    public String getItinerary()                        { return itinerary; }
    public void   setItinerary(String itinerary)        { this.itinerary = itinerary; }

    public String getTitle()                    { return title; }
    public void   setTitle(String title)        { this.title = title; }

    public String getSubtitle()                 { return subtitle; }
    public void   setSubtitle(String subtitle)  { this.subtitle = subtitle; }

    public String getDate()                     { return date; }
    public void   setDate(String date)          { this.date = date; }

    public boolean isActivity()                         { return isActivity; }
    public void    setActivity(boolean activity)        { this.isActivity = activity; }

    public Date getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(Date createdAt)    { this.createdAt = createdAt; }
}
