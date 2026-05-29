package com.example.travelplannerai.ui.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.travelplannerai.R;

import com.example.travelplannerai.data.ai.OpenAIManager;

import com.example.travelplannerai.data.firebase.FirebaseAuthManager;
import com.example.travelplannerai.data.firebase.FirebaseFirestoreManager;
import com.example.travelplannerai.data.model.ChatMessage;
import com.example.travelplannerai.ui.adapters.ChatAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.Map;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private RecyclerView rvChatMessages;
    private EditText etChatMessage;

    private FloatingActionButton fabSendMessage;
    private ProgressBar progressBar;

    private ChatAdapter chatAdapter;
    private OpenAIManager aiManager;
    private FirebaseFirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;

    private String currentChatId;
    private String currentUserId;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_chat, container, false);

        // Initialize managers
        aiManager = OpenAIManager.getInstance();
        firestoreManager = FirebaseFirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize views
        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etChatMessage = view.findViewById(R.id.etChatMessage);

        // Try both button types (MaterialButton or FAB)

        fabSendMessage = view.findViewById(R.id.fabSendMessage);
        fabSendMessage = view.findViewById(R.id.fabSendMessage);
        progressBar = view.findViewById(R.id.progressBar);

        setupRecyclerView();
        setupListeners();
        initializeChat();

        return view;
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Start from bottom

        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupListeners() {

        if (fabSendMessage != null) {
            fabSendMessage.setOnClickListener(v -> sendMessage());
        }
        etChatMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void initializeChat() {
        currentUserId = authManager.getCurrentUserId();

        if (currentUserId == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate unique chat ID
        currentChatId = "chat_" + System.currentTimeMillis();

        Log.d(TAG, "✅ Chat initialized: " + currentChatId);
        Log.d(TAG, "✅ User ID: " + currentUserId);

        // Add welcome message
        ChatMessage welcomeMessage = new ChatMessage(
                "assistant",
                "¡Hola! Soy Travel AI, tu asistente de viajes. ¿En qué puedo ayudarte hoy? Puedo ayudarte a planificar tu viaje, recomendar lugares o responder cualquier pregunta sobre destinos.",
                currentChatId,
                "system"
        );
        chatAdapter.addMessage(welcomeMessage);
    }

    private void sendMessage() {
        String messageText = etChatMessage.getText().toString().trim();

        if (messageText.isEmpty()) {
            Toast.makeText(getContext(), "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear input immediately
        etChatMessage.setText("");

        // Create user message
        ChatMessage userMessage = new ChatMessage("user", messageText, currentChatId, currentUserId);

        // Add to UI
        chatAdapter.addMessage(userMessage);
        scrollToBottom();

        // Save to Firestore
        saveMessageToFirestore(userMessage);

        // Show loading
        showLoading(true);

        // Build trip context (you can enhance this with real trip data)
        String tripContext = "El usuario está planificando un viaje.";

        // Send to AI
        aiManager.sendMessage(messageText, tripContext, new OpenAIManager.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                // Switch to UI thread
                mainHandler.post(() -> {
                    Log.d(TAG, "✅ AI Response received: " + response);

                    // Create AI message
                    ChatMessage aiMessage = new ChatMessage("assistant", response, currentChatId, "ai");

                    // Add to UI
                    chatAdapter.addMessage(aiMessage);
                    scrollToBottom();

                    // Save to Firestore
                    saveMessageToFirestore(aiMessage);

                    // Hide loading
                    showLoading(false);
                });
            }

            @Override
            public void onError(String error) {
                // Switch to UI thread
                mainHandler.post(() -> {
                    Log.e(TAG, "❌ AI Error: " + error);

                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();

                    // Hide loading
                    showLoading(false);
                });
            }
        });
    }

    private void saveMessageToFirestore(ChatMessage message) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", message.getMessageId());
        messageData.put("role", message.getRole());
        messageData.put("content", message.getContent());
        messageData.put("timestamp", message.getTimestamp());
        messageData.put("chatId", message.getChatId());
        messageData.put("userId", message.getUserId());

        firestoreManager.addDocument(
                FirebaseFirestoreManager.COLLECTION_AI_CHATS + "/" + currentChatId + "/messages",
                messageData
        ).addOnSuccessListener(docRef -> {
            Log.d(TAG, "✅ Message saved to Firestore: " + docRef.getId());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Error saving message: " + e.getMessage());
        });
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }



        if (fabSendMessage != null) {
            fabSendMessage.setEnabled(!show);
        }

        etChatMessage.setEnabled(!show);
    }
}
