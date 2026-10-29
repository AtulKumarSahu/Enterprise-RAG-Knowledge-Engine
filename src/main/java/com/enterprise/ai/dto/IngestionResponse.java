package com.enterprise.ai.dto;

public record IngestionResponse(
        String documentId,
        String documentName,
        String accessLevel,
        int totalChunksCreated,
        int totalTokensIndexed,
        String message
) {
}

