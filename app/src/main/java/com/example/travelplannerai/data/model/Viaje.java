package com.example.travelplannerai.data.model;

public class Viaje {
    private String id;
    private String destino;
    private String urlImagen;
    private long fechaInicio;
    private String organizadorId;

    public Viaje() {
    }

    public Viaje(String id, String destino, String urlImagen, long fechaInicio, String organizadorId) {
        this.id = id;
        this.destino = destino;
        this.urlImagen = urlImagen;
        this.fechaInicio = fechaInicio;
        this.organizadorId = organizadorId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public long getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(long fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getOrganizadorId() {
        return organizadorId;
    }

    public void setOrganizadorId(String organizadorId) {
        this.organizadorId = organizadorId;
    }
}
