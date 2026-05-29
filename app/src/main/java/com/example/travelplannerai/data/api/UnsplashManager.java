package com.example.travelplannerai.data.api;

import android.util.Log;

import com.example.travelplannerai.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Singleton Manager para Unsplash API
 * Obtiene fotos automáticas de ciudades
 */
public class UnsplashManager {

    private static final String TAG = "UnsplashManager";


    private static final String UNSPLASH_ACCESS_KEY = BuildConfig.UNSPLASH_API_KEY;

    // Unsplash API Configuration
    private static final String UNSPLASH_API_URL = "https://api.unsplash.com/search/photos";

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
        if (instance == null) {
            instance = new UnsplashManager();
        }
        return instance;
    }

    /**
     * Busca una foto de una ciudad en Unsplash
     *
     * @param city Nombre de la ciudad (ej: "París", "Tokyo")
     * @param callback Callback con la URL de la foto
     */
    public void searchCityPhoto(String city, PhotoCallback callback) {

        // Validar API Key
        if (UNSPLASH_ACCESS_KEY == null || UNSPLASH_ACCESS_KEY.isEmpty()
                || UNSPLASH_ACCESS_KEY.equals("PEGA_TU_UNSPLASH_ACCESS_KEY_AQUI")) {
            Log.e(TAG, "❌ UNSPLASH_ACCESS_KEY no está configurada!");
            callback.onError("Error: Configura tu Unsplash API Key en UnsplashManager.java");
            return;
        }

        Log.d(TAG, "✅ API Key cargada (length: " + UNSPLASH_ACCESS_KEY.length() + ")");

        // Construir URL de búsqueda
        String query = city + " city landscape";
        String encodedQuery = query.replace(" ", "%20");
        String url = UNSPLASH_API_URL + "?query=" + encodedQuery + "&per_page=1&client_id=" + UNSPLASH_ACCESS_KEY;

        Log.d(TAG, "📤 Buscando foto de: " + city);
        Log.d(TAG, "🔗 URL: " + url);

        // Crear request
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept-Version", "v1")
                .build();

        // Ejecutar async
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Error de red: " + e.getMessage(), e);
                callback.onError("Error de conexión: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "❌ Error HTTP: " + response.code());
                    callback.onError("Error HTTP: " + response.code());
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "📥 Respuesta recibida");

                    // Parsear JSON
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray results = jsonResponse.getAsJsonArray("results");

                    if (results == null || results.size() == 0) {
                        Log.w(TAG, "⚠️ No se encontraron fotos");
                        callback.onError("No se encontraron fotos para: " + city);
                        return;
                    }

                    // Obtener primera foto
                    JsonObject photo = results.get(0).getAsJsonObject();
                    JsonObject urls = photo.getAsJsonObject("urls");
                    String photoUrl = urls.get("regular").getAsString();

                    Log.d(TAG, "✅ Foto encontrada!");
                    Log.d(TAG, "🖼️ URL: " + photoUrl);

                    callback.onSuccess(photoUrl);

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parseando JSON: " + e.getMessage(), e);
                    callback.onError("Error parseando respuesta: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Callback para recibir la URL de la foto
     */
    public interface PhotoCallback {
        void onSuccess(String photoUrl);
        void onError(String error);
    }
}