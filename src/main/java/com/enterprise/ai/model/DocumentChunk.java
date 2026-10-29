package com.enterprise.ai.model;

public record DocumentChunk(
        String id,
        String documentId,
        String documentName,
        String content,
        float[] embedding,
        AccessLevel accessLevel,
        int pageNumber,
        int tokenCount
) {
}

