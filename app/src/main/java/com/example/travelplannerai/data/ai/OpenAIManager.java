package com.example.travelplannerai.data.ai;

import android.util.Log;

import com.example.travelplannerai.BuildConfig;
import com.example.travelplannerai.data.api.TokenProvider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Manager para el chat conversacional de viajes.
 *
 * Las llamadas a OpenAI se realizan a través de un Cloud Function proxy
 * que vive en Firebase. La API key de OpenAI NUNCA está en el APK.
 *
 * Flujo:
 *   App → [Firebase ID token] → Cloud Function (chatProxy) → OpenAI → respuesta
 */
public class OpenAIManager {

    private static final String TAG   = "OpenAIManager";
    private static final String MODEL = "gpt-4o-mini";
    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.get("application/json; charset=utf-8");

    /** URL del Cloud Function proxy (definida en local.properties → BuildConfig) */
    private static final String PROXY_URL = BuildConfig.CHAT_PROXY_URL;

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
        if (instance == null) instance = new OpenAIManager();
        return instance;
    }

    // ── System prompt ────────────────────────────────────────────────────────

    public static final String CHAT_SYSTEM_PROMPT =
            "Eres Travel AI, un asistente experto en planificación de viajes integrado en la app TravelPlannerAI.\n\n" +

            "## ESTILO DE CONVERSACIÓN\n" +
            "- Respuestas CORTAS (máx 3-4 líneas). Nunca vuelques información masiva.\n" +
            "- Haz UNA sola pregunta por mensaje.\n" +
            "- Usa emojis con moderación (1-2 por respuesta).\n" +
            "- Responde siempre en ESPAÑOL.\n" +
            "- Sé cálido y cercano, como un amigo que viaja mucho.\n\n" +

            "## PROCESO DE PLANIFICACIÓN\n" +
            "Cuando el usuario quiera planificar un viaje, recopila estos datos UNO A UNO " +
            "si no los ha proporcionado ya:\n" +
            "  1. DESTINO (ciudad o región)\n" +
            "  2. FECHA DE INICIO (formato dd/MM/yyyy)\n" +
            "  3. FECHA DE FIN (formato dd/MM/yyyy)\n" +
            "  4. PRESUPUESTO TOTAL estimado en euros (número entero)\n\n" +

            "## CUANDO TENGAS TODOS LOS DATOS\n" +
            "Muestra un breve resumen del viaje y añade EXACTAMENTE al final de tu respuesta " +
            "(en la última línea, sin nada después) este marcador con JSON válido:\n" +
            "[CREAR_VIAJE:{\"destination\":\"...\",\"startDate\":\"dd/MM/yyyy\"," +
            "\"endDate\":\"dd/MM/yyyy\",\"budget\":1500}]\n\n" +

            "Ejemplo de respuesta con marcador:\n" +
            "¡Perfecto! Ya tengo todo para tu viaje a Calpe 🌊 Del 05/08/2025 al 12/08/2025 " +
            "con un presupuesto de 1.200€. Pulsa el botón para crearlo en la app.\n" +
            "[CREAR_VIAJE:{\"destination\":\"Calpe\",\"startDate\":\"05/08/2025\"," +
            "\"endDate\":\"12/08/2025\",\"budget\":1200}]\n\n" +

            "## OTRAS CONSULTAS\n" +
            "Si el usuario pregunta algo sobre destinos, recomendaciones, clima, etc., " +
            "responde brevemente y ofrece ayudarle a planificar un viaje a ese lugar.";

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Envía la conversación completa al proxy de Cloud Functions.
     * El proxy añade la API key de OpenAI y reenvía la petición.
     *
     * @param history  Historial de mensajes (usuario + asistente), ordenados por tiempo.
     * @param callback Resultado.
     */
    public void sendConversation(
            List<com.example.travelplannerai.data.model.ChatMessage> history,
            ResponseCallback callback) {

        if (PROXY_URL == null || PROXY_URL.isEmpty()) {
            callback.onError("CHAT_PROXY_URL no configurada en local.properties");
            return;
        }

        // Primero obtener el Firebase ID token del usuario actual
        TokenProvider.getToken(new TokenProvider.TokenCallback() {
            @Override
            public void onToken(String idToken) {
                doRequest(history, idToken, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Error de autenticación: " + error);
            }
        });
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private void doRequest(
            List<com.example.travelplannerai.data.model.ChatMessage> history,
            String idToken,
            ResponseCallback callback) {

        // Construir el body igual que OpenAI espera (el proxy lo reenvía tal cual)
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);

        JsonArray messages = new JsonArray();

        // System prompt
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", CHAT_SYSTEM_PROMPT);
        messages.add(sys);

        // Historial completo
        for (com.example.travelplannerai.data.model.ChatMessage msg : history) {
            String role = msg.getRole();
            if (!"user".equals(role) && !"assistant".equals(role)) continue;
            JsonObject m = new JsonObject();
            m.addProperty("role", role);
            m.addProperty("content", msg.getContent());
            messages.add(m);
        }

        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.8);
        requestBody.addProperty("max_tokens", 400);

        String jsonBody = gson.toJson(requestBody);

        Request request = new Request.Builder()
                .url(PROXY_URL)
                .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + idToken)  // token Firebase, no OpenAI
                .build();

        Log.d(TAG, "📤 Enviando al proxy: " + PROXY_URL);

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Error de red: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "📥 Respuesta proxy (" + response.code() + ")");

                if (!response.isSuccessful()) {
                    handleErrorResponse(response.code(), body, callback);
                    return;
                }
                try {
                    callback.onSuccess(parseOpenAIResponse(body));
                } catch (Exception e) {
                    callback.onError("Error al procesar respuesta de IA");
                }
            }
        });
    }

    private String parseOpenAIResponse(String jsonResponse) {
        JsonObject responseObj = gson.fromJson(jsonResponse, JsonObject.class);
        JsonArray choices = responseObj.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new RuntimeException("No choices in response");
        }
        return choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }

    private void handleErrorResponse(int code, String body, ResponseCallback callback) {
        Log.e(TAG, "❌ HTTP " + code + ": " + body);
        switch (code) {
            case 401: callback.onError("Sesión expirada. Vuelve a iniciar sesión."); break;
            case 429: callback.onError("Demasiadas peticiones. Espera un momento."); break;
            case 500:
            case 503: callback.onError("Error del servidor. Intenta de nuevo."); break;
            default:  callback.onError("Error inesperado (código " + code + ")"); break;
        }
    }

    /**
     * Envía un mensaje único con contexto opcional (usado en TripDetailFragment
     * para generar el itinerario de un viaje concreto).
     *
     * Internamente construye una conversación de un solo turno y la manda al proxy.
     *
     * @param userMessage Mensaje / prompt del usuario
     * @param tripContext Contexto del viaje (destino, fechas, presupuesto) o null
     * @param callback    Resultado
     */
    public void sendMessage(String userMessage, String tripContext, ResponseCallback callback) {
        if (PROXY_URL == null || PROXY_URL.isEmpty()) {
            callback.onError("CHAT_PROXY_URL no configurada en local.properties");
            return;
        }

        TokenProvider.getToken(new TokenProvider.TokenCallback() {
            @Override
            public void onToken(String idToken) {
                // Construir body con system prompt de itinerario + mensaje del usuario
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", MODEL);

                JsonArray messages = new JsonArray();

                // System prompt para generación de itinerario
                String systemPrompt = "Eres un asistente experto en viajes. "
                        + "Genera itinerarios detallados, claros y útiles en español. "
                        + "Usa formato legible con secciones por día.";
                if (tripContext != null && !tripContext.isEmpty()) {
                    systemPrompt += "\n\nContexto del viaje:\n" + tripContext;
                }

                JsonObject sys = new JsonObject();
                sys.addProperty("role", "system");
                sys.addProperty("content", systemPrompt);
                messages.add(sys);

                JsonObject user = new JsonObject();
                user.addProperty("role", "user");
                user.addProperty("content", userMessage);
                messages.add(user);

                requestBody.add("messages", messages);
                requestBody.addProperty("temperature", 0.7);
                requestBody.addProperty("max_tokens", 1024);

                String jsonBody = gson.toJson(requestBody);

                Request request = new Request.Builder()
                        .url(PROXY_URL)
                        .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Bearer " + idToken)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        callback.onError("Error de red: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String body = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            handleErrorResponse(response.code(), body, callback);
                            return;
                        }
                        try {
                            callback.onSuccess(parseOpenAIResponse(body));
                        } catch (Exception e) {
                            callback.onError("Error al procesar respuesta de IA");
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError("Error de autenticación: " + error);
            }
        });
    }

    public interface ResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
