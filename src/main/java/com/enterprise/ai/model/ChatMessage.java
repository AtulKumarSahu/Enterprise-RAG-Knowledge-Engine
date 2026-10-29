package com.enterprise.ai.model;

public record ChatMessage(
        String role,
        String content,
        long timestamp
) {
    public ChatMessage(String role, String content) {
        this(role, content, System.currentTimeMillis());
    }
}

