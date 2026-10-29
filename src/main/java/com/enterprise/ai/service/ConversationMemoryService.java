package com.enterprise.ai.service;

import com.enterprise.ai.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class ConversationMemoryService {

    private static final int MAX_HISTORY_PER_SESSION = 8;
    private final Map<String, Deque<ChatMessage>> sessionStore = new ConcurrentHashMap<>();

    public void addMessage(String sessionId, String role, String content) {
        if (sessionId == null || sessionId.isBlank() || content == null || content.isBlank()) {
            return;
        }

        Deque<ChatMessage> history = sessionStore.computeIfAbsent(sessionId, k -> new ConcurrentLinkedDeque<>());
        history.addLast(new ChatMessage(role, content));

        // Enforce rolling window
        while (history.size() > MAX_HISTORY_PER_SESSION) {
            history.pollFirst();
        }
    }

    public List<ChatMessage> getHistory(String sessionId) {
        if (sessionId == null || !sessionStore.containsKey(sessionId)) {
            return List.of();
        }
        return new ArrayList<>(sessionStore.get(sessionId));
    }

    public String getRecentContext(String sessionId) {
        List<ChatMessage> history = getHistory(sessionId);
        if (history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : history) {
            sb.append(msg.role()).append(": ").append(msg.content()).append(" ");
        }
        return sb.toString().trim();
    }

    public boolean clearSession(String sessionId) {
        return sessionStore.remove(sessionId) != null;
    }

    public int getActiveSessionCount() {
        return sessionStore.size();
    }
}

