package com.example.travelplannerai.data.ai;

import android.util.Log;

import com.example.travelplannerai.BuildConfig;
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
 * Singleton Manager for OpenAI API (GPT-4o-mini)
 * Much more stable than Gemini!
 */
public class OpenAIManager {

    private static final String TAG = "OpenAIManager";

    // ⚠️ REPLACE WITH YOUR OPENAI API KEY ⚠️
    // Get it from: https://platform.openai.com/api-keys
    private static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY;

    // OpenAI API Configuration
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini"; // Fast and cheap model
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private static OpenAIManager instance;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private OpenAIManager() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        gson = new Gson();
    }

    public static synchronized OpenAIManager getInstance() {
        if (instance == null) {
            instance = new OpenAIManager();
        }
        return instance;
    }

    /**
     * Send message to OpenAI GPT
     *
     * @param userMessage User's input message
     * @param tripContext Optional context about the trip (can be null)
     * @param callback Response callback
     */
    public void sendMessage(String userMessage, String tripContext, ResponseCallback callback) {

        // Validate API Key
        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isEmpty() || OPENAI_API_KEY.equals("PEGA_TU_OPENAI_KEY_AQUI")) {
            Log.e(TAG, "❌ OPENAI_API_KEY is not configured!");
            callback.onError("Error de configuración: Debes reemplazar OPENAI_API_KEY en OpenAIManager.java");
            return;
        }

        Log.d(TAG, "✅ API Key loaded (length: " + OPENAI_API_KEY.length() + ")");

        // Build system prompt
        String systemPrompt = buildSystemPrompt(tripContext);

        // Build request JSON (OpenAI format)
        JsonObject requestBody = buildOpenAIRequestBody(systemPrompt, userMessage);
        String jsonBody = gson.toJson(requestBody);

        Log.d(TAG, "📤 Sending request to OpenAI");
        Log.d(TAG, "📦 Request body: " + jsonBody);

        // Create HTTP POST request
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
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
                    String aiMessage = parseOpenAIResponse(responseBody);
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
        prompt.append("Tus respuestas deben ser específicas y accionables.");

        if (tripContext != null && !tripContext.isEmpty()) {
            prompt.append("\n\nContexto del viaje actual:\n").append(tripContext);
        }

        return prompt.toString();
    }

    /**
     * Build request body with OpenAI format
     */
    private JsonObject buildOpenAIRequestBody(String systemPrompt, String userMessage) {
        JsonObject requestBody = new JsonObject();

        // Model
        requestBody.addProperty("model", MODEL);

        // Messages array
        JsonArray messages = new JsonArray();

        // System message
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);
        }

        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        // Parameters
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 1024);

        return requestBody;
    }

    /**
     * Parse OpenAI API response
     */
    private String parseOpenAIResponse(String jsonResponse) {
        try {
            JsonObject responseObj = gson.fromJson(jsonResponse, JsonObject.class);

            JsonArray choices = responseObj.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                throw new Exception("No choices in response");
            }

            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");

            return message.get("content").getAsString();

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
            case 401:
                errorMessage = "API Key inválida. Verifica tu clave de OpenAI.";
                Log.e(TAG, "❌ 401 Unauthorized - API Key issue: " + body);
                break;
            case 429:
                errorMessage = "Demasiadas peticiones. Espera un momento.";
                Log.e(TAG, "❌ 429 Too Many Requests: " + body);
                break;
            case 500:
            case 503:
                errorMessage = "Error del servidor de OpenAI. Intenta de nuevo.";
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