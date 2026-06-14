package com.example.travelplannerai.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Map;

/**
 * Singleton Manager para Cloud Firestore
 */
public class FirebaseFirestoreManager {

    private static FirebaseFirestoreManager instance;
    private final FirebaseFirestore db;

    // Nombres de colecciones
    public static final String COLLECTION_USERS        = "users";
    public static final String COLLECTION_TRIPS        = "trips";
    public static final String COLLECTION_EXCURSIONS   = "excursions";
    public static final String COLLECTION_PLACES       = "places";
    public static final String COLLECTION_ITINERARIES  = "itineraries";
    public static final String COLLECTION_FAVORITES    = "favorites";
    public static final String COLLECTION_PARTICIPANTS = "participants";
    public static final String COLLECTION_AI_CHATS     = "ai_chats";
    public static final String COLLECTION_SAVED_PLACES = "saved_places";

    private FirebaseFirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseFirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirebaseFirestoreManager();
        }
        return instance;
    }

    // ==================== MÉTODOS GENÉRICOS ====================

    public CollectionReference getCollection(String collectionPath) {
        return db.collection(collectionPath);
    }

    public DocumentReference getDocument(String collectionPath, String documentId) {
        return db.collection(collectionPath).document(documentId);
    }

    public Task<DocumentReference> addDocument(String collectionPath, Map<String, Object> data) {
        return db.collection(collectionPath).add(data);
    }

    public Task<Void> setDocument(String collectionPath, String documentId, Map<String, Object> data) {
        return db.collection(collectionPath).document(documentId).set(data);
    }

    public Task<Void> updateDocument(String collectionPath, String documentId, Map<String, Object> updates) {
        return db.collection(collectionPath).document(documentId).update(updates);
    }

    public Task<Void> deleteDocument(String collectionPath, String documentId) {
        return db.collection(collectionPath).document(documentId).delete();
    }

    public Task<DocumentSnapshot> getDocumentById(String collectionPath, String documentId) {
        return db.collection(collectionPath).document(documentId).get();
    }

    public Task<QuerySnapshot> getAllDocuments(String collectionPath) {
        return db.collection(collectionPath).get();
    }

    public Task<QuerySnapshot> getDocumentsWhere(String collectionPath, String field, Object value) {
        return db.collection(collectionPath)
                .whereEqualTo(field, value)
                .get();
    }

    public Task<QuerySnapshot> getDocumentsOrderedBy(String collectionPath, String field, Query.Direction direction) {
        return db.collection(collectionPath)
                .orderBy(field, direction)
                .get();
    }

    public WriteBatch createBatch() {
        return db.batch();
    }

    // ==================== USUARIOS ====================

    public Task<Void> createUser(String userId, Map<String, Object> userData) {
        return setDocument(COLLECTION_USERS, userId, userData);
    }

    public Task<DocumentSnapshot> getUser(String userId) {
        return getDocumentById(COLLECTION_USERS, userId);
    }

    public Task<Void> updateUser(String userId, Map<String, Object> updates) {
        return updateDocument(COLLECTION_USERS, userId, updates);
    }

    // ==================== VIAJES ====================

    public Task<QuerySnapshot> getUserTrips(String userId) {
        return getDocumentsWhere(COLLECTION_TRIPS, "userId", userId);
    }

    public Task<DocumentReference> createTrip(Map<String, Object> tripData) {
        return addDocument(COLLECTION_TRIPS, tripData);
    }

    public Task<DocumentSnapshot> getTrip(String tripId) {
        return getDocumentById(COLLECTION_TRIPS, tripId);
    }

    public Task<Void> updateTrip(String tripId, Map<String, Object> updates) {
        return updateDocument(COLLECTION_TRIPS, tripId, updates);
    }

    public Task<Void> deleteTrip(String tripId) {
        // Limpieza en cascada (best-effort, en segundo plano) para no dejar
        // excursiones ni favoritos huérfanos apuntando a un viaje borrado.
        cleanupTripSubdata(tripId);
        return deleteDocument(COLLECTION_TRIPS, tripId);
    }

    /**
     * Borra los datos asociados a un viaje: su subcolección de excursiones
     * (trips/{tripId}/excursions) y cualquier entrada en favoritos con ese tripId.
     */
    private void cleanupTripSubdata(String tripId) {
        db.collection(COLLECTION_TRIPS).document(tripId).collection("excursions").get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) d.getReference().delete();
                });

        db.collection(COLLECTION_FAVORITES).whereEqualTo("tripId", tripId).get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) d.getReference().delete();
                });
    }

    // ==================== EXCURSIONES ====================

    public Task<QuerySnapshot> getTripExcursions(String tripId) {
        return getDocumentsWhere(COLLECTION_EXCURSIONS, "tripId", tripId);
    }

    public Task<DocumentReference> createExcursion(Map<String, Object> excursionData) {
        return addDocument(COLLECTION_EXCURSIONS, excursionData);
    }

    public Task<Void> updateExcursion(String excursionId, Map<String, Object> updates) {
        return updateDocument(COLLECTION_EXCURSIONS, excursionId, updates);
    }

    public Task<Void> deleteExcursion(String excursionId) {
        return deleteDocument(COLLECTION_EXCURSIONS, excursionId);
    }

    // ==================== FAVORITOS ====================

    public Task<QuerySnapshot> getUserFavorites(String userId) {
        return getDocumentsWhere(COLLECTION_FAVORITES, "userId", userId);
    }

    public Task<DocumentReference> addToFavorites(Map<String, Object> favoriteData) {
        return addDocument(COLLECTION_FAVORITES, favoriteData);
    }

    public Task<Void> removeFromFavorites(String favoriteId) {
        return deleteDocument(COLLECTION_FAVORITES, favoriteId);
    }

    // ==================== LUGARES GUARDADOS ====================

    /** Devuelve todos los lugares guardados por el usuario. */
    public Task<QuerySnapshot> getUserSavedPlaces(String userId) {
        return getDocumentsWhere(COLLECTION_SAVED_PLACES, "userId", userId);
    }

    /** Guarda un lugar en la colección saved_places. */
    public Task<DocumentReference> savePlace(Map<String, Object> placeData) {
        return addDocument(COLLECTION_SAVED_PLACES, placeData);
    }

    /** Elimina un lugar guardado por su ID de documento. */
    public Task<Void> deletePlace(String placeId) {
        return deleteDocument(COLLECTION_SAVED_PLACES, placeId);
    }
}
