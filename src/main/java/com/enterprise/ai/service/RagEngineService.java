package com.enterprise.ai.service;

import com.enterprise.ai.dto.ChatRequest;
import com.enterprise.ai.dto.ChatResponse;
import com.enterprise.ai.dto.CitationSource;
import com.enterprise.ai.model.AccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagEngineService {

    private static final Logger log = LoggerFactory.getLogger(RagEngineService.class);

    private final VectorStoreService vectorStoreService;
    private final TokenQuotaService tokenQuotaService;
    private final ConversationMemoryService conversationMemoryService;

    @Value("${app.ai.rag.top-k:3}")
    private int defaultTopK;

    public RagEngineService(VectorStoreService vectorStoreService,
                            TokenQuotaService tokenQuotaService,
                            ConversationMemoryService conversationMemoryService) {
        this.vectorStoreService = vectorStoreService;
        this.tokenQuotaService = tokenQuotaService;
        this.conversationMemoryService = conversationMemoryService;
    }

    public ChatResponse processQuery(ChatRequest request) {
        String question = request.question();
        String userId = request.userId();
        String sessionId = request.sessionId();
        AccessLevel userRole = AccessLevel.fromString(request.userRole());

        // 1. Quota Pre-Check
        int estimatedQueryTokens = Math.max(1, question.split("\\s+").length) + 150;
        if (!tokenQuotaService.hasAvailableQuota(userId, estimatedQueryTokens)) {
            return new ChatResponse(
                    question,
                    "Token quota exceeded for user '" + userId + "'. Please contact your workspace administrator.",
                    List.of(),
                    0,
                    false,
                    sessionId
            );
        }

        // 2. Vector Similarity Search with Role Filter and Multi-Turn Fallback
        List<VectorStoreService.ScoredChunk> relevantScoredChunks = vectorStoreService.similaritySearch(question, defaultTopK, userRole);

        // If direct query has no matches, check if conversation memory can resolve pronouns or short follow-ups
        if (relevantScoredChunks.isEmpty()) {
            String recentContext = conversationMemoryService.getRecentContext(sessionId);
            if (!recentContext.isBlank()) {
                String contextualQuery = question + " " + recentContext;
                relevantScoredChunks = vectorStoreService.similaritySearch(contextualQuery, defaultTopK, userRole);
            }
        }

        if (relevantScoredChunks.isEmpty()) {
            tokenQuotaService.consumeTokens(userId, estimatedQueryTokens);
            conversationMemoryService.addMessage(sessionId, "USER", question);
            String refusalMsg = "No relevant company documentation found matching your query for clearance role: " + userRole.name() + ".";
            conversationMemoryService.addMessage(sessionId, "ASSISTANT", refusalMsg);

            return new ChatResponse(
                    question,
                    refusalMsg,
                    List.of(),
                    estimatedQueryTokens,
                    false,
                    sessionId
            );
        }

        // 3. Assemble Grounded Citations
        List<CitationSource> citations = new ArrayList<>();
        StringBuilder answerBuilder = new StringBuilder();
        answerBuilder.append("Based on company documentation matching your clearance (").append(userRole.name()).append("):\n\n");

        for (VectorStoreService.ScoredChunk scored : relevantScoredChunks) {
            var chunk = scored.chunk();
            citations.add(new CitationSource(
                    chunk.documentName(),
                    chunk.pageNumber(),
                    Math.round(scored.score() * 1000.0) / 1000.0,
                    chunk.content().length() > 180 ? chunk.content().substring(0, 180) + "..." : chunk.content()
            ));

            answerBuilder.append("• From [").append(chunk.documentName()).append("] (Page ").append(chunk.pageNumber()).append("):\n");
            answerBuilder.append("  ").append(chunk.content()).append("\n\n");
        }

        String finalAnswer = answerBuilder.toString().trim();
        int totalTokensUsed = estimatedQueryTokens + (finalAnswer.length() / 4);
        tokenQuotaService.consumeTokens(userId, totalTokensUsed);

        // Record turn in session memory
        conversationMemoryService.addMessage(sessionId, "USER", question);
        conversationMemoryService.addMessage(sessionId, "ASSISTANT", finalAnswer);

        log.info("[RAG-ENGINE] Query resolved with {} citations for user '{}' (role: {}, session: {})",
                citations.size(), userId, userRole.name(), sessionId);

        return new ChatResponse(
                question,
                finalAnswer,
                citations,
                totalTokensUsed,
                true,
                sessionId
        );
    }
}
