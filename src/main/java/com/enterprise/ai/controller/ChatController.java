package com.enterprise.ai.controller;

import com.enterprise.ai.dto.ChatRequest;
import com.enterprise.ai.dto.ChatResponse;
import com.enterprise.ai.model.ChatMessage;
import com.enterprise.ai.service.ConversationMemoryService;
import com.enterprise.ai.service.RagEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final RagEngineService ragEngineService;
    private final ConversationMemoryService conversationMemoryService;

    public ChatController(RagEngineService ragEngineService, ConversationMemoryService conversationMemoryService) {
        this.ragEngineService = ragEngineService;
        this.conversationMemoryService = conversationMemoryService;
    }

    @PostMapping("/query")
    public ResponseEntity<ChatResponse> queryAssistant(@RequestBody ChatRequest request) {
        ChatResponse response = ragEngineService.processQuery(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getSessionHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(conversationMemoryService.getHistory(sessionId));
    }

    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSessionHistory(@PathVariable String sessionId) {
        boolean cleared = conversationMemoryService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "cleared", cleared,
                "message", cleared ? "Session conversation history cleared successfully" : "Session not found"
        ));
    }
}
