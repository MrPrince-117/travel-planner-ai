package com.example.travelplannerai.data.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Modelo para los mensajes del chat con la IA.
 */
public class ChatMessage {
    @DocumentId
    private String messageId;
    private String senderId; // "user" o "ia"
    private String text;
    private long timestamp;

    public ChatMessage() {
        // Requerido por Firebase
    }

    public ChatMessage(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
