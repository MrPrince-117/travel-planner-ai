package com.example.travelplannerai.data.api;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Obtiene el Firebase ID Token del usuario actual de forma síncrona.
 *
 * El token se usa como "Authorization: Bearer {token}" al llamar a las
 * Cloud Functions proxy, que lo verifican en el servidor antes de reenviar
 * la petición a OpenAI / Unsplash.
 *
 * Esto garantiza que solo usuarios autenticados en la app puedan usar
 * las APIs, sin exponer ninguna key en el APK.
 */
public class TokenProvider {

    public interface TokenCallback {
        void onToken(String idToken);
        void onError(String error);
    }

    /**
     * Obtiene el ID token del usuario logueado.
     * Siempre fuerza refresh=false para no tardar más de lo necesario.
     */
    public static void getToken(TokenCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Usuario no autenticado");
            return;
        }
        user.getIdToken(false)
            .addOnSuccessListener(result -> callback.onToken(result.getToken()))
            .addOnFailureListener(e  -> callback.onError("Error obteniendo token: " + e.getMessage()));
    }
}
