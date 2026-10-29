package com.enterprise.ai.controller;

import com.enterprise.ai.dto.QuotaResponse;
import com.enterprise.ai.service.TokenQuotaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quota")
public class QuotaController {

    private final TokenQuotaService tokenQuotaService;

    public QuotaController(TokenQuotaService tokenQuotaService) {
        this.tokenQuotaService = tokenQuotaService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<QuotaResponse> getUserQuota(@PathVariable String userId) {
        QuotaResponse quota = tokenQuotaService.getQuota(userId);
        return ResponseEntity.ok(quota);
    }
}

