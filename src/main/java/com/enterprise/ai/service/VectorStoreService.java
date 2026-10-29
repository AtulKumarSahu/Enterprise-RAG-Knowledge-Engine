package com.enterprise.ai.service;

import com.enterprise.ai.model.AccessLevel;
import com.enterprise.ai.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "because", "as", "what",
            "which", "this", "that", "these", "those", "then", "just", "so", "than",
            "such", "both", "through", "about", "for", "is", "of", "while", "during",
            "to", "from", "in", "out", "on", "off", "over", "under", "again", "further",
            "once", "here", "there", "when", "where", "why", "how", "all", "any",
            "each", "few", "more", "most", "other", "some", "no", "nor",
            "not", "only", "own", "same", "too", "very", "can", "will", "should",
            "now", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "having", "do", "does", "did", "doing", "i", "you", "he", "she", "it",
            "we", "they", "me", "him", "her", "us", "them", "tell", "give", "with",
            "whats", "doesnt"
    );

    private final EmbeddingService embeddingService;
    private final Map<String, DocumentChunk> vectorIndex = new ConcurrentHashMap<>();

    @Value("${app.ai.rag.min-similarity:0.20}")
    private double minSimilarity;

    public record ScoredChunk(DocumentChunk chunk, double score) {}

    public VectorStoreService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public void saveChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        for (DocumentChunk chunk : chunks) {
            vectorIndex.put(chunk.id(), chunk);
        }
        log.info("[VECTOR-STORE] Stored {} new chunks. Total indexed chunks: {}", chunks.size(), vectorIndex.size());
    }

    public List<ScoredChunk> similaritySearch(String query, int topK, AccessLevel userRole) {
        if (query == null || query.trim().isEmpty() || vectorIndex.isEmpty()) {
            return List.of();
        }

        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        Set<String> queryKeywords = extractKeywords(query);

        return vectorIndex.values().stream()
                // Role-based metadata filtering: user must have equal or higher clearance
                .filter(chunk -> userRole.canAccess(chunk.accessLevel()))
                // Calculate hybrid score: dense semantic cosine similarity + sparse keyword overlap
                .map(chunk -> {
                    double denseScore = Math.max(0.0, embeddingService.calculateCosineSimilarity(queryEmbedding, chunk.embedding()));
                    double sparseScore = calculateSparseOverlap(queryKeywords, chunk.content());

                    // Noise suppression: if no sparse lexical overlap exists and dense similarity is below confidence, treat as non-match
                    if (sparseScore == 0.0 && denseScore < 0.40) {
                        return new ScoredChunk(chunk, 0.0);
                    }

                    // 50% Dense + 50% Sparse Hybrid RAG Fusion
                    double hybridScore = (0.50 * denseScore) + (0.50 * sparseScore);
                    return new ScoredChunk(chunk, hybridScore);
                })
                // Filter by minimum similarity score
                .filter(scored -> scored.score() >= minSimilarity)
                // Sort descending by similarity
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private double calculateSparseOverlap(Set<String> queryKeywords, String content) {
        if (queryKeywords.isEmpty() || content == null || content.isBlank()) {
            return 0.0;
        }
        Set<String> contentTokens = extractKeywords(content);
        if (contentTokens.isEmpty()) {
            return 0.0;
        }

        long matchedTokens = queryKeywords.stream()
                .filter(token -> contentTokens.stream().anyMatch(ct -> ct.equals(token) || (token.length() > 3 && ct.startsWith(token)) || (ct.length() > 3 && token.startsWith(ct))))
                .count();

        return (double) matchedTokens / queryKeywords.size();
    }

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9\\s]", " ").split("\\s+");
        for (String w : words) {
            if (w.length() >= 3 && !STOP_WORDS.contains(w)) {
                keywords.add(w);
            }
        }
        return keywords;
    }

    public int getTotalIndexedChunks() {
        return vectorIndex.size();
    }

    public void clear() {
        vectorIndex.clear();
    }
}
