package com.enterprise.ai.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;

@Service
public class EmbeddingService {

    public static final int EMBEDDING_DIMENSIONS = 128;

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
            "we", "they", "me", "him", "her", "us", "them"
    );

    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[EMBEDDING_DIMENSIONS];
        }

        float[] vector = new float[EMBEDDING_DIMENSIONS];
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9\\s]", " ").split("\\s+");

        int validTokens = 0;
        for (String word : words) {
            if (word.isBlank() || STOP_WORDS.contains(word)) {
                continue;
            }
            validTokens++;
            accumulateWordHash(vector, word);
        }

        if (validTokens == 0) {
            for (String word : words) {
                if (word.isBlank()) continue;
                accumulateWordHash(vector, word);
            }
        }

        // L2 Normalization
        float norm = 0.0f;
        for (float val : vector) {
            norm += val * val;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0.0f) {
            for (int i = 0; i < EMBEDDING_DIMENSIONS; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    private void accumulateWordHash(float[] vector, String word) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(word.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < EMBEDDING_DIMENSIONS; i++) {
                int byteIndex = i % hash.length;
                float weight = (float) ((hash[byteIndex] & 0xFF) - 128) / 128.0f;
                vector[i] += weight;
            }
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 guaranteed in Java SE
        }
    }

    public double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
