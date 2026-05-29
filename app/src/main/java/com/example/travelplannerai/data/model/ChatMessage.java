package com.example.travelplannerai.data.model;

public class ChatMessage {
    private String messageId;
    private String role;        // "user" o "assistant"
    private String content;
    private long timestamp;
    private String chatId;      // FK to AI_CHATS collection
    private String userId;

    // Constructor vacío para Firestore
    public ChatMessage() {
    }

    // Constructor completo
    public ChatMessage(String messageId, String role, String content, long timestamp, String chatId, String userId) {
        this.messageId = messageId;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.chatId = chatId;
        this.userId = userId;
    }

    // Constructor rápido para crear mensajes
    public ChatMessage(String role, String content, String chatId, String userId) {
        this.messageId = String.valueOf(System.currentTimeMillis());
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.chatId = chatId;
        this.userId = userId;
    }

    // Getters y Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // Helper methods
    public boolean isUserMessage() {
        return "user".equals(role);
    }

    public boolean isAssistantMessage() {
        return "assistant".equals(role);
    }
}
