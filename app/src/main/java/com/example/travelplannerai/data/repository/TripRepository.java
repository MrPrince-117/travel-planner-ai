package com.example.travelplannerai.data.repository;

import com.example.travelplannerai.data.firebase.FirebaseHelper;
import com.example.travelplannerai.data.model.Trip;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Repositorio de la capa de datos para gestionar operaciones de Firestore
 * asociadas a la colección de viajes ("viajes").
 */
public class TripRepository {

    private static TripRepository instance;
    private final FirebaseFirestore db;
    private final CollectionReference tripsCollection;

    private TripRepository() {
        // Inicializa Firestore a través de FirebaseHelper Singleton
        db = FirebaseHelper.getInstance().getFirestore();
        tripsCollection = db.collection("viajes");
    }

    /**
     * Devuelve la única instancia del TripRepository (Singleton).
     */
    public static synchronized TripRepository getInstance() {
        if (instance == null) {
            instance = new TripRepository();
        }
        return instance;
    }

    /**
     * Guarda un viaje (objeto Trip) en la colección "viajes" de Firestore.
     *
     * @param trip El viaje POJO a guardar.
     * @return Una tarea de Google Play Services Task<DocumentReference> para gestionar el éxito o fracaso de la escritura.
     */
    public Task<DocumentReference> saveTrip(Trip trip) {
        return tripsCollection.add(trip);
    }

    /**
     * Obtiene todos los viajes de la colección "viajes" en Firestore.
     *
     * @param listener El callback OnCompleteListener para recibir los resultados (QuerySnapshot).
     */
    public void getTrips(OnCompleteListener<QuerySnapshot> listener) {
        tripsCollection.get()
                .addOnCompleteListener(listener);
    }
}
