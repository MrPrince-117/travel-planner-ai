package com.example.travelplannerai.data.ai;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Singleton Manager for Google Gemini AI API
 *
 * IMPORTANT: Replace GEMINI_API_KEY with your actual API key from Google AI Studio
 * https://aistudio.google.com/app/apikey
 */
public class GoogleAIManager {

    private static final String TAG = "GoogleAIManager";

    // ⚠️ REPLACE THIS WITH YOUR ACTUAL API KEY ⚠️
    // Get your key from: https://aistudio.google.com/app/apikey
    private static final String GEMINI_API_KEY = "";

    // Gemini API Configuration
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1/models/";
    private static final String MODEL_NAME = "gemini-1.5-flash-latest";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private static GoogleAIManager instance;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private GoogleAIManager() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        gson = new Gson();
    }

    public static synchronized GoogleAIManager getInstance() {
        if (instance == null) {
            instance = new GoogleAIManager();
        }
        return instance;
    }

    /**
     * Send message to Gemini AI
     *
     * @param userMessage User's input message
     * @param tripContext Optional context about the trip (can be null)
     * @param callback Response callback
     */
    public void sendMessage(String userMessage, String tripContext, ResponseCallback callback) {

        // Validate API Key
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isEmpty() || GEMINI_API_KEY.equals("PEGA_TU_API_KEY_AQUI")) {
            Log.e(TAG, "❌ GEMINI_API_KEY is not configured!");
            callback.onError("Error de configuración: Debes reemplazar GEMINI_API_KEY en GoogleAIManager.java con tu clave de Google AI Studio");
            return;
        }

        Log.d(TAG, "✅ API Key loaded (length: " + GEMINI_API_KEY.length() + ")");

        // Build endpoint with API key
        String endpoint = GEMINI_BASE_URL + MODEL_NAME + ":generateContent?key=" + GEMINI_API_KEY;

        // Build system prompt
        String systemPrompt = buildSystemPrompt(tripContext);

        // Build request JSON (CORRECT STRUCTURE FOR GEMINI)
        JsonObject requestBody = buildGeminiRequestBody(systemPrompt, userMessage);
        String jsonBody = gson.toJson(requestBody);

        Log.d(TAG, "📤 Sending request to Gemini");
        Log.d(TAG, "📦 Request body: " + jsonBody);

        // Create HTTP POST request
        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute async
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Network error: " + e.getMessage(), e);
                callback.onError("Error de red: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "📥 Response code: " + response.code());
                Log.d(TAG, "📥 Response body: " + responseBody);

                if (!response.isSuccessful()) {
                    handleErrorResponse(response.code(), responseBody, callback);
                    return;
                }

                try {
                    String aiMessage = parseGeminiResponse(responseBody);
                    Log.d(TAG, "✅ AI Response parsed successfully");
                    callback.onSuccess(aiMessage);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parsing response: " + e.getMessage(), e);
                    callback.onError("Error al procesar respuesta de IA");
                }
            }
        });
    }

    /**
     * Build system prompt with trip context
     */
    private String buildSystemPrompt(String tripContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un asistente de viajes experto y amigable llamado Travel AI. ");
        prompt.append("Ayudas a los usuarios a planificar sus viajes, recomendar lugares y crear itinerarios. ");
        prompt.append("Responde siempre en español, de forma clara, concisa y útil. ");
        prompt.append("Tus respuestas deben ser específicas y accionables. ");

        if (tripContext != null && !tripContext.isEmpty()) {
            prompt.append("\n\nContexto del viaje actual:\n").append(tripContext);
        }

        return prompt.toString();
    }

    /**
     * Build request body with CORRECT Gemini API structure
     */
    private JsonObject buildGeminiRequestBody(String systemPrompt, String userMessage) {
        JsonObject requestBody = new JsonObject();

        // System instruction (recommended way for Gemini)
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemInstruction = new JsonObject();

            JsonArray systemParts = new JsonArray();
            JsonObject systemPart = new JsonObject();
            systemPart.addProperty("text", systemPrompt);
            systemParts.add(systemPart);

            systemInstruction.add("parts", systemParts);
            requestBody.add("system_instruction", systemInstruction);
        }

        // Contents array with user message
        JsonArray contents = new JsonArray();

        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");

        JsonArray userParts = new JsonArray();
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userMessage);
        userParts.add(userPart);

        userContent.add("parts", userParts);
        contents.add(userContent);

        requestBody.add("contents", contents);

        // Generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("maxOutputTokens", 1024);
        generationConfig.addProperty("topP", 0.95);
        requestBody.add("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Parse Gemini API response
     */
    private String parseGeminiResponse(String jsonResponse) {
        try {
            JsonObject responseObj = gson.fromJson(jsonResponse, JsonObject.class);

            JsonArray candidates = responseObj.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                throw new Exception("No candidates in response");
            }

            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject content = firstCandidate.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");

            if (parts == null || parts.size() == 0) {
                throw new Exception("No parts in response");
            }

            JsonObject firstPart = parts.get(0).getAsJsonObject();
            return firstPart.get("text").getAsString();

        } catch (Exception e) {
            Log.e(TAG, "Error parsing response structure: " + e.getMessage());
            throw new RuntimeException("Invalid response format");
        }
    }

    /**
     * Handle HTTP error responses
     */
    private void handleErrorResponse(int code, String body, ResponseCallback callback) {
        String errorMessage;

        switch (code) {
            case 400:
                errorMessage = "Solicitud inválida. Revisa el formato del mensaje.";
                Log.e(TAG, "❌ 400 Bad Request: " + body);
                break;
            case 403:
                errorMessage = "API Key inválida o bloqueada. Genera una nueva clave en Google AI Studio.";
                Log.e(TAG, "❌ 403 Forbidden - API Key issue: " + body);
                break;
            case 404:
                errorMessage = "Modelo no encontrado. Verifica el nombre del modelo.";
                Log.e(TAG, "❌ 404 Not Found: " + body);
                break;
            case 429:
                errorMessage = "Demasiadas peticiones. Espera un momento e intenta de nuevo.";
                Log.e(TAG, "❌ 429 Too Many Requests: " + body);
                break;
            case 500:
            case 503:
                errorMessage = "Error del servidor de Google. Intenta de nuevo en unos segundos.";
                Log.e(TAG, "❌ " + code + " Server Error: " + body);
                break;
            default:
                errorMessage = "Error desconocido (código " + code + ")";
                Log.e(TAG, "❌ HTTP " + code + ": " + body);
        }

        callback.onError(errorMessage);
    }

    /**
     * Callback interface for async responses
     */
    public interface ResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}