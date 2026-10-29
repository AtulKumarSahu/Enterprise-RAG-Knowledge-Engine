package com.enterprise.ai.dto;

import java.util.List;

public record ChatResponse(
        String question,
        String answer,
        List<CitationSource> sources,
        int tokensUsed,
        boolean grounded,
        String sessionId
) {
    public ChatResponse(String question, String answer, List<CitationSource> sources, int tokensUsed, boolean grounded) {
        this(question, answer, sources, tokensUsed, grounded, "session-default");
    }
}
