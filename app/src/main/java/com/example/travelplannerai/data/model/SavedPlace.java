package com.example.travelplannerai.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Lugar guardado desde la pantalla "Explorar Lugares".
 * Se persiste en la colección Firestore "saved_places".
 */
public class SavedPlace {

    @DocumentId
    private String id;

    private String userId;
    private String name;
    private String address;
    private String category;
    private double lat;
    private double lng;

    @ServerTimestamp
    private Date savedAt;

    /** Constructor vacío requerido por Firestore. */
    public SavedPlace() { }

    public SavedPlace(String userId, String name, String address,
                      String category, double lat, double lng) {
        this.userId   = userId;
        this.name     = name;
        this.address  = address;
        this.category = category;
        this.lat      = lat;
        this.lng      = lng;
    }

    // ── Getters y setters ────────────────────────────────────────────────────

    public String getId()                    { return id; }
    public void   setId(String id)           { this.id = id; }

    public String getUserId()                { return userId; }
    public void   setUserId(String userId)   { this.userId = userId; }

    public String getName()                  { return name; }
    public void   setName(String name)       { this.name = name; }

    public String getAddress()               { return address; }
    public void   setAddress(String address) { this.address = address; }

    public String getCategory()                  { return category; }
    public void   setCategory(String category)   { this.category = category; }

    public double getLat()                   { return lat; }
    public void   setLat(double lat)         { this.lat = lat; }

    public double getLng()                   { return lng; }
    public void   setLng(double lng)         { this.lng = lng; }

    public Date getSavedAt()                 { return savedAt; }
    public void setSavedAt(Date savedAt)     { this.savedAt = savedAt; }
}
