package com.example.travelplannerai.ui.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelplannerai.R;
import com.example.travelplannerai.data.ai.OpenAIManager;
import com.example.travelplannerai.data.api.UnsplashManager;
import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.example.travelplannerai.data.model.ChatMessage;
import com.example.travelplannerai.ui.adapters.ChatAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pantalla de chat con Travel AI.
 *
 * Características:
 *  - Conversación progresiva: la IA guía al usuario con UNA pregunta a la vez
 *    para definir destino, fechas y presupuesto.
 *  - Historial completo enviado a OpenAI en cada turno (la IA "recuerda").
 *  - Detección automática del marcador [CREAR_VIAJE:{...}] en la respuesta de la IA.
 *  - Al detectarlo → muestra una card con el resumen del viaje y botón para crearlo.
 *  - El viaje se guarda en Firestore igual que si se creara manualmente.
 */
public class ChatFragment extends Fragment {

    private static final String TAG            = "ChatFragment";
    private static final String TRIP_MARKER    = "CREAR_VIAJE";
    private static final Pattern TRIP_PATTERN  =
            Pattern.compile("\\[" + TRIP_MARKER + ":(\\{.*?\\})\\]",
                    Pattern.DOTALL);

    // ── Vistas ────────────────────────────────────────────────────────────────
    private RecyclerView       rvChatMessages;
    private TextInputEditText  etChatMessage;
    private FloatingActionButton fabSendMessage;
    private ProgressBar        progressBar;
    private MaterialButton     btnNewChat;
    private MaterialCardView   cardCreateTrip;
    private TextView           tvTripSummary;
    private MaterialButton     btnCreateTrip;

    // ── Datos ─────────────────────────────────────────────────────────────────
    private ChatAdapter   chatAdapter;
    private OpenAIManager aiManager;
    private FirebaseFirestoreManager firestoreManager;
    private FirebaseAuthManager      authManager;

    private String       currentChatId;
    private String       currentUserId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Historial de la conversación que se envía a OpenAI.
     * Solo contiene mensajes reales (user + assistant), nunca el saludo de bienvenida.
     */
    private final List<ChatMessage> conversationHistory = new ArrayList<>();

    /** Datos del viaje detectado por el marcador, null si aún no se han detectado. */
    private DetectedTrip detectedTrip = null;

    // ── Modelo auxiliar para viaje detectado ──────────────────────────────────

    private static class DetectedTrip {
        String destination;
        String startDate;
        String endDate;
        int    budget;

        DetectedTrip(String destination, String startDate, String endDate, int budget) {
            this.destination = destination;
            this.startDate   = startDate;
            this.endDate     = endDate;
            this.budget      = budget;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Ciclo de vida
    // ════════════════════════════════════════════════════════════════════════

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ai_chat, container, false);

        aiManager        = OpenAIManager.getInstance();
        firestoreManager = FirebaseFirestoreManager.getInstance();
        authManager      = FirebaseAuthManager.getInstance();

        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etChatMessage  = view.findViewById(R.id.etChatMessage);
        fabSendMessage = view.findViewById(R.id.fabSendMessage);
        progressBar    = view.findViewById(R.id.progressBar);
        btnNewChat     = view.findViewById(R.id.btnNewChat);
        cardCreateTrip = view.findViewById(R.id.cardCreateTrip);
        tvTripSummary  = view.findViewById(R.id.tvTripSummary);
        btnCreateTrip  = view.findViewById(R.id.btnCreateTrip);

        setupRecyclerView();
        setupListeners();
        initializeChat();

        return view;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Configuración
    // ════════════════════════════════════════════════════════════════════════

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        lm.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(lm);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        fabSendMessage.setOnClickListener(v -> sendMessage());

        etChatMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });

        btnNewChat.setOnClickListener(v -> startNewChat());

        // Descartar la card del viaje detectado
        cardCreateTrip.findViewById(R.id.btnDismissTrip).setOnClickListener(v -> {
            cardCreateTrip.setVisibility(View.GONE);
            detectedTrip = null;
        });

        // Crear viaje desde la conversación
        btnCreateTrip.setOnClickListener(v -> createTripFromChat());
    }

    private void initializeChat() {
        currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Error: sesión no iniciada", Toast.LENGTH_SHORT).show();
            return;
        }
        currentChatId = "chat_" + System.currentTimeMillis();
        conversationHistory.clear();
        cardCreateTrip.setVisibility(View.GONE);
        detectedTrip = null;

        // Mensaje de bienvenida (solo UI, NO entra en el historial de OpenAI)
        addAiMessageToUI("¡Hola! 👋 Soy Travel AI, tu asistente de viajes.\n" +
                "¿Estás pensando en algún destino? Cuéntame y te ayudo a planificarlo paso a paso.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Envío de mensajes
    // ════════════════════════════════════════════════════════════════════════

    private void sendMessage() {
        String text = etChatMessage.getText() != null
                ? etChatMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        etChatMessage.setText("");

        // Añadir al historial y a la UI
        ChatMessage userMsg = new ChatMessage("user", text, currentChatId, currentUserId);
        conversationHistory.add(userMsg);
        chatAdapter.addMessage(userMsg);
        scrollToBottom();
        saveMessageToFirestore(userMsg);

        showLoading(true);

        // Enviar historial completo a OpenAI
        aiManager.sendConversation(conversationHistory,
                new OpenAIManager.ResponseCallback() {
                    @Override
                    public void onSuccess(String response) {
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            showLoading(false);

                            // Detectar marcador de viaje
                            DetectedTrip trip = extractTripData(response);
                            String cleanResponse = stripTripMarker(response);

                            // Añadir respuesta limpia al historial y UI
                            ChatMessage aiMsg = new ChatMessage(
                                    "assistant", cleanResponse, currentChatId, "ai");
                            conversationHistory.add(aiMsg);
                            chatAdapter.addMessage(aiMsg);
                            scrollToBottom();
                            saveMessageToFirestore(aiMsg);

                            // Mostrar card si se detectó un viaje
                            if (trip != null) {
                                detectedTrip = trip;
                                showTripCard(trip);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            showLoading(false);
                            Toast.makeText(getContext(),
                                    "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Detección del marcador [CREAR_VIAJE:{...}]
    // ════════════════════════════════════════════════════════════════════════

    @Nullable
    private DetectedTrip extractTripData(String response) {
        Matcher m = TRIP_PATTERN.matcher(response);
        if (!m.find()) return null;
        try {
            JsonObject json = new Gson().fromJson(m.group(1), JsonObject.class);
            String destination = json.has("destination")
                    ? json.get("destination").getAsString() : null;
            String startDate   = json.has("startDate")
                    ? json.get("startDate").getAsString()   : null;
            String endDate     = json.has("endDate")
                    ? json.get("endDate").getAsString()     : null;
            int    budget      = json.has("budget")
                    ? json.get("budget").getAsInt()         : 0;

            if (destination == null || startDate == null || endDate == null) return null;
            return new DetectedTrip(destination, startDate, endDate, budget);
        } catch (Exception e) {
            Log.e(TAG, "Error parseando datos del viaje: " + e.getMessage());
            return null;
        }
    }

    private String stripTripMarker(String response) {
        return TRIP_PATTERN.matcher(response).replaceAll("").trim();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Card "Crear viaje"
    // ════════════════════════════════════════════════════════════════════════

    private void showTripCard(DetectedTrip trip) {
        String summary = "📍 " + trip.destination
                + "   ·   📅 " + trip.startDate + " → " + trip.endDate
                + "   ·   💰 " + formatBudget(trip.budget) + "€";
        tvTripSummary.setText(summary);
        cardCreateTrip.setVisibility(View.VISIBLE);
    }

    private String formatBudget(int budget) {
        // Añadir separadores de miles: 1200 → 1.200
        return String.format(java.util.Locale.getDefault(), "%,d", budget)
                .replace(',', '.');
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Crear viaje en Firestore
    // ════════════════════════════════════════════════════════════════════════

    private void createTripFromChat() {
        if (detectedTrip == null) return;
        if (currentUserId == null) return;

        btnCreateTrip.setEnabled(false);
        btnCreateTrip.setText("Creando viaje...");

        // Primero buscar foto del destino en Unsplash, luego guardar
        UnsplashManager.getInstance().searchCityPhoto(
                detectedTrip.destination,
                new UnsplashManager.PhotoCallback() {
                    @Override
                    public void onSuccess(String photoUrl) {
                        mainHandler.post(() -> saveTrip(photoUrl));
                    }
                    @Override
                    public void onError(String error) {
                        // Guardar igualmente sin foto
                        mainHandler.post(() -> saveTrip(""));
                    }
                }
        );
    }

    private void saveTrip(String imageUrl) {
        if (detectedTrip == null || !isAdded()) return;

        Map<String, Object> tripData = new HashMap<>();
        tripData.put("userId",      currentUserId);
        tripData.put("destination", detectedTrip.destination);
        tripData.put("dates",       detectedTrip.startDate + " - " + detectedTrip.endDate);
        tripData.put("budget",      (double) detectedTrip.budget);
        tripData.put("imageUrl",    imageUrl);

        firestoreManager.createTrip(tripData)
                .addOnSuccessListener(docRef -> {
                    if (!isAdded()) return;

                    // Ocultar card
                    cardCreateTrip.setVisibility(View.GONE);
                    detectedTrip = null;

                    // Mensaje de confirmación en el chat
                    String confirmMsg = "✅ ¡Viaje a " + tripData.get("destination")
                            + " creado! Ya puedes verlo en \"Mis Viajes\". "
                            + "¿Quieres que genere un itinerario para este viaje?";
                    addAiMessageToUI(confirmMsg);

                    Toast.makeText(getContext(),
                            "✈️ Viaje creado correctamente", Toast.LENGTH_SHORT).show();

                    // Navegar a Mis Viajes
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_chatFragment_to_myTripsFragment);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnCreateTrip.setEnabled(true);
                    btnCreateTrip.setText("Crear viaje en Mis Viajes");
                    Toast.makeText(getContext(),
                            "Error al crear el viaje", Toast.LENGTH_SHORT).show();
                });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Nueva conversación
    // ════════════════════════════════════════════════════════════════════════

    private void startNewChat() {
        conversationHistory.clear();
        chatAdapter.clearMessages();
        cardCreateTrip.setVisibility(View.GONE);
        detectedTrip = null;
        currentChatId = "chat_" + System.currentTimeMillis();
        addAiMessageToUI("¡Chat reiniciado! 🔄 ¿A dónde te gustaría viajar?");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers de UI
    // ════════════════════════════════════════════════════════════════════════

    private void addAiMessageToUI(String text) {
        ChatMessage msg = new ChatMessage("assistant", text, currentChatId, "ai");
        chatAdapter.addMessage(msg);
        scrollToBottom();
    }

    private void scrollToBottom() {
        int count = chatAdapter.getItemCount();
        if (count > 0) rvChatMessages.smoothScrollToPosition(count - 1);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        fabSendMessage.setEnabled(!show);
        etChatMessage.setEnabled(!show);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Persistencia en Firestore
    // ════════════════════════════════════════════════════════════════════════

    private void saveMessageToFirestore(ChatMessage message) {
        Map<String, Object> data = new HashMap<>();
        data.put("messageId", message.getMessageId());
        data.put("role",      message.getRole());
        data.put("content",   message.getContent());
        data.put("timestamp", message.getTimestamp());
        data.put("chatId",    message.getChatId());
        data.put("userId",    message.getUserId());

        firestoreManager.addDocument(
                FirebaseFirestoreManager.COLLECTION_AI_CHATS
                        + "/" + currentChatId + "/messages", data)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error guardando mensaje: " + e.getMessage()));
    }
}
