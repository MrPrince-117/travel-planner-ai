package com.example.travelplannerai.data.api;

import android.util.Log;

import com.example.travelplannerai.BuildConfig;
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
 * Manager para la Foursquare Places API.
 * Documentación: https://docs.foursquare.com/developer/reference/place-search
 */
public class FoursquareManager {

    private static final String TAG        = "FoursquareManager";
    private static final String API_KEY = BuildConfig.FOURSQUARE_API_KEY;
    private static final String BASE_URL   = "https://api.foursquare.com/v3/places/search";

    private static FoursquareManager instance;
    private final OkHttpClient httpClient;

    // Modelo de lugar
    public static class Place {
        public String fsqId;
        public String name;
        public String address;
        public String category;
        public double lat;
        public double lng;

        public Place(String fsqId, String name, String address, String category,
                     double lat, double lng) {
            this.fsqId    = fsqId;
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

    private FoursquareManager() {
        httpClient = new OkHttpClient();
    }

    public static synchronized FoursquareManager getInstance() {
        if (instance == null) instance = new FoursquareManager();
        return instance;
    }

    /**
     * Busca lugares por texto y categoría.
     * @param query     Texto de búsqueda (ej: "restaurantes Roma")
     * @param near      Ciudad o zona (ej: "Roma, Italia")
     * @param category  Categoría Foursquare: "hotel", "restaurant", "museum" o null para todo
     * @param callback  Resultado
     */
    public void searchPlaces(String query, String near, String category,
                             PlacesCallback callback) {

        StringBuilder urlBuilder = new StringBuilder(BASE_URL);
        urlBuilder.append("?query=").append(encode(query));
        urlBuilder.append("&near=").append(encode(near));
        urlBuilder.append("&limit=15");
        urlBuilder.append("&fields=fsq_id,name,location,categories,geocodes");

        if (category != null && !category.isEmpty()) {
            // Foursquare category IDs
            switch (category) {
                case "hotel":
                    urlBuilder.append("&categories=19014"); break;
                case "restaurant":
                    urlBuilder.append("&categories=13000"); break;
                case "museum":
                    urlBuilder.append("&categories=10027"); break;
            }
        }

        Request request = new Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("Authorization", "fsq3" + API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        Log.d(TAG, "🔍 Buscando: " + urlBuilder);

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Error de red: " + e.getMessage());
                callback.onError("Error de red: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "📥 Response " + response.code() + ": " + body.substring(0, Math.min(200, body.length())));

                if (!response.isSuccessful()) {
                    callback.onError("Error " + response.code() + ": " + body);
                    return;
                }

                try {
                    List<Place> places = parsePlaces(body);
                    callback.onSuccess(places);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parseando respuesta: " + e.getMessage());
                    callback.onError("Error al procesar los resultados");
                }
            }
        });
    }

    private List<Place> parsePlaces(String json) {
        List<Place> places = new ArrayList<>();

        JsonObject root    = JsonParser.parseString(json).getAsJsonObject();
        JsonArray  results = root.getAsJsonArray("results");

        if (results == null) return places;

        for (int i = 0; i < results.size(); i++) {
            try {
                JsonObject item = results.get(i).getAsJsonObject();

                String fsqId = item.has("fsq_id") ? item.get("fsq_id").getAsString() : "";
                String name  = item.has("name")   ? item.get("name").getAsString()   : "Sin nombre";

                // Dirección
                String address = "";
                if (item.has("location")) {
                    JsonObject loc = item.getAsJsonObject("location");
                    if (loc.has("formatted_address"))
                        address = loc.get("formatted_address").getAsString();
                    else if (loc.has("address"))
                        address = loc.get("address").getAsString();
                }

                // Categoría
                String category = "";
                if (item.has("categories")) {
                    JsonArray cats = item.getAsJsonArray("categories");
                    if (cats.size() > 0) {
                        JsonObject cat = cats.get(0).getAsJsonObject();
                        if (cat.has("name")) category = cat.get("name").getAsString();
                    }
                }

                // Coordenadas
                double lat = 0, lng = 0;
                if (item.has("geocodes")) {
                    JsonObject geo  = item.getAsJsonObject("geocodes");
                    JsonObject main = geo.has("main") ? geo.getAsJsonObject("main") : null;
                    if (main != null) {
                        lat = main.has("latitude")  ? main.get("latitude").getAsDouble()  : 0;
                        lng = main.has("longitude") ? main.get("longitude").getAsDouble() : 0;
                    }
                }

                places.add(new Place(fsqId, name, address, category, lat, lng));

            } catch (Exception e) {
                Log.w(TAG, "⚠️ Error parseando lugar " + i + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "✅ " + places.size() + " lugares parseados");
        return places;
    }

    private String encode(String text) {
        try {
            return java.net.URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return text;
        }
    }
}
