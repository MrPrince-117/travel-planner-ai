package com.example.travelplannerai.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Singleton Manager para Firebase Authentication
 */
public class FirebaseAuthManager {

    private static FirebaseAuthManager instance;
    private final FirebaseAuth mAuth;

    private FirebaseAuthManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    // Registrar usuario
    public Task<AuthResult> registerUser(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password);
    }

    // Iniciar sesión
    public Task<AuthResult> loginUser(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password);
    }

    // Cerrar sesión
    public void logout() {
        mAuth.signOut();
    }

    // Obtener usuario actual
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    // Verificar si hay sesión activa
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    // Obtener UID del usuario
    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // Recuperar contraseña
    public Task<Void> sendPasswordResetEmail(String email) {
        return mAuth.sendPasswordResetEmail(email);
    }

    // Actualizar contraseña
    public Task<Void> updatePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.updatePassword(newPassword);
        }
        return null;
    }

    // Eliminar cuenta
    public Task<Void> deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.delete();
        }
        return null;
    }

    // Enviar email de verificación
    public Task<Void> sendEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.sendEmailVerification();
        }
        return null;
    }
}
