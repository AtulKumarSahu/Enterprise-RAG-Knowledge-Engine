package com.enterprise.ai.config;

import com.enterprise.ai.model.AccessLevel;
import com.enterprise.ai.service.DocumentIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner seedSampleKnowledge(DocumentIngestionService ingestionService) {
        return args -> {
            log.info("[SEED] Seeding enterprise documents with tiered role access...");

            // 1. PUBLIC Access Document
            String travelPolicy = """
                    GLOBAL COMPANY TRAVEL AND EXPENSE POLICY (2026 REVISION)
                    Section 1: Airfare and Rail Travel
                    All domestic flights under 4 hours must be booked in Economy class. International flights over 7 hours are eligible for Premium Economy or Business Class with VP approval.
                    Section 2: Lodging and Meals
                    Hotel rates are capped at $220 per night in tier-1 cities (San Francisco, New York, London, Tokyo) and $160 in all other locations.
                    The daily meal stipend is $85 per day. Alcoholic beverages during individual dinners are not reimbursable.
                    Section 3: Ground Transportation and Rideshare
                    Uber, Lyft, and standard airport taxis are fully reimbursable. Rental cars require prior manager approval.
                    Section 4: Expense Submission Deadline
                    All expense receipts must be submitted via the finance portal within 14 calendar days of trip completion. Receipts over $25 must include an itemized breakdown.
                    """;
            ingestionService.ingestText("Company_Travel_Policy_2026.txt", travelPolicy, AccessLevel.PUBLIC);

            // 2. ENGINEERING Access Document
            String engRunbook = """
                    ENGINEERING PRODUCTION INCIDENT AND ON-CALL RUNBOOK (CONFIDENTIAL)
                    Section 1: P1 Critical Incident Protocol
                    A P1 incident is declared when customer-facing latency exceeds 1.5 seconds or error rates exceed 2% across 3 consecutive evaluation cycles.
                    Upon declaration, the primary on-call engineer must open an incident bridge in Slack (#incidents-p1) within 5 minutes.
                    Section 2: Automated Rollback Procedure
                    To trigger an emergency rollback of a failing microservice, execute:
                    kubectl rollout undo deployment/api-gateway -n production
                    For canary releases, immediately set canary weight to 0% in Istio VirtualService.
                    Section 3: Database Failover
                    In the event of primary PostgreSQL master degradation, promote the hot standby replica using:
                    pg_ctl promote -D /var/lib/postgresql/data
                    Verify write operations succeed before updating the connection pool endpoint in Vault.
                    """;
            ingestionService.ingestText("Engineering_Production_Runbook.txt", engRunbook, AccessLevel.ENGINEERING);

            // 3. FINANCE Access Document
            String financeSalaries = """
                    EXECUTIVE COMPENSATION AND TARGET BONUS BANDS (RESTRICTED FINANCE)
                    Section 1: Annual Bonus Pool Distribution
                    The executive bonus pool is funded based on reaching 115% of annual recurring revenue (ARR) milestones.
                    Level E1 (Directors): Target bonus is 25% of base salary, evaluated on 50% company performance and 50% team OKRs.
                    Level E2 (Vice Presidents): Target bonus is 40% of base salary, with 70% weighted to enterprise EBITDA targets.
                    Level E3 (Senior VPs and C-Suite): Target bonus is 60% of base salary plus discretionary equity performance grants.
                    Section 2: Stock Option Vesting
                    Executive equity grants follow a 4-year standard schedule with a 1-year cliff (25%), followed by monthly equal tranches.
                    Accelerated double-trigger vesting occurs only in change-of-control acquisitions.
                    """;
            ingestionService.ingestText("Executive_Compensation_Bands.txt", financeSalaries, AccessLevel.FINANCE);

            log.info("[SEED] Seeding complete! All documents indexed into vector store with role gating.");
        };
    }
}

