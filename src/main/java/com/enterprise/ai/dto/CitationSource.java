package com.enterprise.ai.dto;

public record CitationSource(
        String documentName,
        int pageNumber,
        double relevanceScore,
        String excerpt
) {
}

