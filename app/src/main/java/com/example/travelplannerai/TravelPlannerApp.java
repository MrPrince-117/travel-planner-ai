package com.example.travelplannerai;

import android.app.Application;

import com.example.travelplannerai.utils.ThemeManager;

/**
 * Clase Application de la app.
 *
 * Su única responsabilidad por ahora es aplicar el tema (claro/oscuro)
 * guardado por el usuario ANTES de que se cree cualquier Activity, evitando
 * el "parpadeo" de arrancar en un tema y cambiar después.
 */
public class TravelPlannerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Aplica la preferencia de tema persistida (por defecto: claro).
        ThemeManager.applySavedTheme(this);
    }
}
