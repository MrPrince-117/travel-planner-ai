package com.example.travelplannerai.data.api;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Manager para Nominatim (OpenStreetMap) — búsqueda de lugares gratuita sin API key.
 * Documentación: https://nominatim.org/release-docs/develop/api/Search/
 */
public class NominatimManager {

    private static final String TAG      = "NominatimManager";
    private static final String BASE_URL = "https://nominatim.openstreetmap.org/search";

    private static NominatimManager instance;
    private final OkHttpClient httpClient;

    // Modelo de lugar
    public static class Place {
        public String name;
        public String address;
        public String category;
        public double lat;
        public double lng;

        /**
         * URL de imagen cargada desde Unsplash.
         * Se rellena la primera vez que el adapter la pide y se cachea aquí,
         * así el scroll no repite llamadas a la API.
         * null = todavía no se ha buscado, "" = se buscó pero no se encontró.
         */
        public String imageUrl = null;

        public Place(String name, String address, String category, double lat, double lng) {
            this.name     = name;
            this.address  = address;
            this.category = category;
            this.lat      = lat;
            this.lng      = lng;
        }
    }

    public interface PlacesCallback {
        void onSuccess(List<Place> places);
        void onError(String error);
    }

    private NominatimManager() {
        httpClient = new OkHttpClient();
    }

    public static synchronized NominatimManager getInstance() {
        if (instance == null) instance = new NominatimManager();
        return instance;
    }

    /**
     * Busca lugares por texto.
     * @param query    Texto de búsqueda (ej: "restaurantes Madrid" o "museos París")
     * @param callback Resultado
     */
    public void searchPlaces(String query, PlacesCallback callback) {

        String url = BASE_URL
                + "?q=" + encode(query)
                + "&format=json"
                + "&limit=15"
                + "&addressdetails=1";

        Request request = new Request.Builder()
                .url(url)
                // Nominatim requiere un User-Agent identificativo
                .addHeader("User-Agent", "TravelPlannerAI/1.0 (Android)")
                .addHeader("Accept-Language", "es")
                .build();

        Log.d(TAG, "🔍 Buscando: " + url);

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Error de red: " + e.getMessage());
                callback.onError("Error de red: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "📥 Response " + response.code() + ": "
                        + body.substring(0, Math.min(300, body.length())));

                if (!response.isSuccessful()) {
                    callback.onError("Error " + response.code());
                    return;
                }

                try {
                    List<Place> places = parsePlaces(body);
                    callback.onSuccess(places);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parseando: " + e.getMessage());
                    callback.onError("Error al procesar resultados");
                }
            }
        });
    }

    private List<Place> parsePlaces(String json) {
        List<Place> places = new ArrayList<>();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        for (int i = 0; i < array.size(); i++) {
            try {
                JsonObject item = array.get(i).getAsJsonObject();

                // Nombre del lugar
                String name = item.has("name") && !item.get("name").getAsString().isEmpty()
                        ? item.get("name").getAsString()
                        : item.get("display_name").getAsString().split(",")[0];

                // Dirección completa
                String displayName = item.has("display_name")
                        ? item.get("display_name").getAsString() : "";

                // Simplificar dirección: tomar los primeros 3 segmentos
                String[] parts   = displayName.split(",");
                StringBuilder sb = new StringBuilder();
                int limit        = Math.min(3, parts.length);
                for (int j = 0; j < limit; j++) {
                    if (j > 0) sb.append(",");
                    sb.append(parts[j].trim());
                }
                String address = sb.toString();

                // Categoría / tipo
                String category = item.has("type")
                        ? capitalize(item.get("type").getAsString().replace("_", " "))
                        : "Lugar";

                // Coordenadas
                double lat = item.has("lat") ? Double.parseDouble(item.get("lat").getAsString()) : 0;
                double lng = item.has("lon") ? Double.parseDouble(item.get("lon").getAsString()) : 0;

                places.add(new Place(name, address, category, lat, lng));

            } catch (Exception e) {
                Log.w(TAG, "⚠️ Error parseando lugar " + i + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "✅ " + places.size() + " lugares parseados");
        return places;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String encode(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }
}
