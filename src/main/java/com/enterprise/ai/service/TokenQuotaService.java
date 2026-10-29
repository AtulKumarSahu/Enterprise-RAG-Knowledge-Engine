package com.enterprise.ai.service;

import com.enterprise.ai.dto.QuotaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TokenQuotaService {

    private static final Logger log = LoggerFactory.getLogger(TokenQuotaService.class);

    @Value("${app.ai.quota.default-daily-tokens:50000}")
    private int dailyTokenLimit;

    private final Map<String, AtomicInteger> userUsage = new ConcurrentHashMap<>();

    public boolean hasAvailableQuota(String userId, int estimatedCost) {
        AtomicInteger usage = userUsage.computeIfAbsent(userId, k -> new AtomicInteger(0));
        return (usage.get() + estimatedCost) <= dailyTokenLimit;
    }

    public int consumeTokens(String userId, int tokens) {
        AtomicInteger usage = userUsage.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int totalUsed = usage.addAndGet(tokens);
        log.info("[QUOTA] User '{}' consumed {} tokens. Total today: {} / {}", userId, tokens, totalUsed, dailyTokenLimit);
        return totalUsed;
    }

    public QuotaResponse getQuota(String userId) {
        AtomicInteger usage = userUsage.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int used = usage.get();
        int remaining = Math.max(0, dailyTokenLimit - used);
        String status = remaining > 0 ? "ACTIVE" : "EXHAUSTED";

        return new QuotaResponse(userId, used, dailyTokenLimit, remaining, status);
    }
}

