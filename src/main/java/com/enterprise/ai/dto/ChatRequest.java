package com.enterprise.ai.dto;

public record ChatRequest(
        String question,
        String userRole,
        String userId,
        String sessionId
) {
    public ChatRequest {
        if (userRole == null || userRole.trim().isEmpty()) {
            userRole = "PUBLIC";
        }
        if (userId == null || userId.trim().isEmpty()) {
            userId = "default-user";
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "session-default";
        }
    }

    public ChatRequest(String question, String userRole, String userId) {
        this(question, userRole, userId, "session-default");
    }
}
