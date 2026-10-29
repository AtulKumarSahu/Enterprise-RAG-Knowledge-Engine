package com.enterprise.ai.dto;

public record QuotaResponse(
        String userId,
        int tokensUsedToday,
        int dailyTokenLimit,
        int remainingTokens,
        String status
) {
}

