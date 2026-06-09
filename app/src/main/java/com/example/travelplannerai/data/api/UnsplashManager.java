package com.example.travelplannerai.data.api;

import android.util.Log;

import com.example.travelplannerai.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Manager para obtener fotos de lugares/ciudades.
 *
 * Las llamadas a Unsplash se realizan a través del Cloud Function proxy.
 * La API key de Unsplash NUNCA está en el APK.
 *
 * Flujo:
 *   App → [Firebase ID token] → Cloud Function (photoProxy) → Unsplash → URL
 */
public class UnsplashManager {

    private static final String TAG = "UnsplashManager";

    /** URL del Cloud Function proxy (definida en local.properties → BuildConfig) */
    private static final String PROXY_URL = BuildConfig.PHOTO_PROXY_URL;

    private static UnsplashManager instance;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private UnsplashManager() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    public static synchronized UnsplashManager getInstance() {
        if (instance == null) instance = new UnsplashManager();
        return instance;
    }

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Busca una foto de una ciudad (para tarjetas de viaje y portadas de trip).
     *
     * @param city     Nombre de la ciudad (ej: "París", "Tokyo")
     * @param callback Resultado con la URL de la foto
     */
    public void searchCityPhoto(String city, PhotoCallback callback) {
        searchPhoto(city + " city landscape", callback);
    }

    /**
     * Búsqueda genérica por cualquier término.
     * Usada por el adapter de resultados de Explorar Lugares.
     *
     * @param query    Texto de búsqueda (ej: "Hotel Regina Louvre hotel")
     * @param callback Resultado con la URL de la foto
     */
    public void searchPhoto(String query, PhotoCallback callback) {
        if (PROXY_URL == null || PROXY_URL.isEmpty()) {
            callback.onError("PHOTO_PROXY_URL no configurada en local.properties");
            return;
        }

        // Obtener Firebase ID token y luego hacer la petición al proxy
        TokenProvider.getToken(new TokenProvider.TokenCallback() {
            @Override
            public void onToken(String idToken) {
                doRequest(query, idToken, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Error de autenticación: " + error);
            }
        });
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private void doRequest(String query, String idToken, PhotoCallback callback) {
        String encoded = encode(query);
        String url = PROXY_URL + "?query=" + encoded;

        Log.d(TAG, "📤 Buscando foto: " + query);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Error de red: " + e.getMessage());
                callback.onError("Error de red: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "❌ Error HTTP: " + response.code());
                    callback.onError("Error HTTP: " + response.code());
                    return;
                }
                try {
                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);

                    // El proxy devuelve { "url": "https://..." } o { "url": null }
                    if (json.has("url") && !json.get("url").isJsonNull()) {
                        String photoUrl = json.get("url").getAsString();
                        Log.d(TAG, "✅ Foto encontrada");
                        callback.onSuccess(photoUrl);
                    } else {
                        Log.w(TAG, "⚠️ Sin resultados para: " + query);
                        callback.onError("Sin resultados");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parseando respuesta: " + e.getMessage());
                    callback.onError("Error al parsear respuesta");
                }
            }
        });
    }

    private String encode(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return text.replace(" ", "%20");
        }
    }

    public interface PhotoCallback {
        void onSuccess(String photoUrl);
        void onError(String error);
    }
}
