package com.example.travelplannerai.data.firebase;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Singleton Helper para inicializar y obtener la instancia central de Firebase Firestore.
 */
public class FirebaseHelper {

    private static FirebaseHelper instance;
    private final FirebaseFirestore db;

    private FirebaseHelper() {
        // Inicializa la base de datos Firestore
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Devuelve la única instancia del FirebaseHelper (Singleton).
     */
    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    /**
     * Obtiene la instancia activa de FirebaseFirestore.
     */
    public FirebaseFirestore getFirestore() {
        return db;
    }
}
