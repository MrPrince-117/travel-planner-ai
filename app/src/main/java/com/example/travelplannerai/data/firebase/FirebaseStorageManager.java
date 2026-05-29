package com.example.travelplannerai.data.firebase;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * Singleton Manager para Firebase Storage
 */
public class FirebaseStorageManager {

    private static FirebaseStorageManager instance;
    private final FirebaseStorage storage;
    private final StorageReference storageRef;

    // Carpetas en Storage
    public static final String FOLDER_PROFILE_IMAGES = "profile_images";
    public static final String FOLDER_TRIP_IMAGES    = "trip_images";
    public static final String FOLDER_PLACE_IMAGES   = "place_images";

    private FirebaseStorageManager() {
        storage    = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    public static synchronized FirebaseStorageManager getInstance() {
        if (instance == null) {
            instance = new FirebaseStorageManager();
        }
        return instance;
    }

    // Subir imagen de perfil
    public UploadTask uploadProfileImage(String userId, Uri imageUri) {
        StorageReference profileImagesRef = storageRef.child(FOLDER_PROFILE_IMAGES + "/" + userId + ".jpg");
        return profileImagesRef.putFile(imageUri);
    }

    // Subir imagen de viaje
    public UploadTask uploadTripImage(String tripId, Uri imageUri) {
        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference tripImagesRef = storageRef.child(FOLDER_TRIP_IMAGES + "/" + tripId + "/" + fileName);
        return tripImagesRef.putFile(imageUri);
    }

    // Subir imagen de lugar
    public UploadTask uploadPlaceImage(String placeId, Uri imageUri) {
        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference placeImagesRef = storageRef.child(FOLDER_PLACE_IMAGES + "/" + placeId + "/" + fileName);
        return placeImagesRef.putFile(imageUri);
    }

    // Obtener URL de imagen de perfil
    public Task<Uri> getProfileImageUrl(String userId) {
        StorageReference profileImageRef = storageRef.child(FOLDER_PROFILE_IMAGES + "/" + userId + ".jpg");
        return profileImageRef.getDownloadUrl();
    }

    // Obtener URL de imagen de viaje
    public Task<Uri> getTripImageUrl(String tripId, String fileName) {
        StorageReference tripImageRef = storageRef.child(FOLDER_TRIP_IMAGES + "/" + tripId + "/" + fileName);
        return tripImageRef.getDownloadUrl();
    }

    // Eliminar imagen de perfil
    public Task<Void> deleteProfileImage(String userId) {
        StorageReference profileImageRef = storageRef.child(FOLDER_PROFILE_IMAGES + "/" + userId + ".jpg");
        return profileImageRef.delete();
    }

    // Eliminar imágenes de viaje
    public Task<Void> deleteTripImages(String tripId) {
        StorageReference tripFolderRef = storageRef.child(FOLDER_TRIP_IMAGES + "/" + tripId);
        return tripFolderRef.delete();
    }

    public StorageReference getStorageReference() {
        return storageRef;
    }
}
