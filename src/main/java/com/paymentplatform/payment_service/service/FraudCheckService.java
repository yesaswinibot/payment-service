package com.paymentplatform.payment_service.service;

import com.paymentplatform.payment_service.dto.FraudCheckResult;
import com.paymentplatform.payment_service.entity.Transaction;
import com.paymentplatform.payment_service.enums.RiskLevel;
import com.paymentplatform.payment_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCheckService {

    private static final TransactionRepository transactionRepository = null;

    public static FraudCheckResult check(Transaction txn){
        List<String> reasons = new ArrayList<>();
        List<String> triggeredRules = new ArrayList<>();

        // Skip fraud check if merchant is whitelisted
        if (isWhitelisted(txn)) {
            return FraudCheckResult.builder()
                    .transactionId(txn.getId())
                    .isFraud(false)
                    .reasons(reasons)
                    .triggeredRules(triggeredRules)
                    .riskLevel(RiskLevel.LOW)
                    .checkedAt(LocalDateTime.now())
                    .build();
        }

        // Run all rules and collect results
        if (checkHighValue(txn)) {
            triggeredRules.add("HIGH_VALUE");
            reasons.add("Transaction amount exceeds ₹5,00,000 threshold");
        }

        if (checkVelocity(txn)) {
            triggeredRules.add("VELOCITY_EXCEEDED");
            reasons.add("More than 5 transactions in last 5 minutes");
        }

        if (checkUnusualHour(txn)) {
            triggeredRules.add("UNUSUAL_HOUR");
            reasons.add("Transaction initiated between 12am and 5am");
        }

        // Calculate risk level based on how many rules triggered
        RiskLevel riskLevel = triggeredRules.isEmpty() ? RiskLevel.LOW :
                triggeredRules.size() == 1 ? RiskLevel.MEDIUM :
                        RiskLevel.HIGH;

        return FraudCheckResult.builder()
                .transactionId(txn.getId())
                .isFraud(!triggeredRules.isEmpty())
                .reasons(reasons)
                .triggeredRules(triggeredRules)
                .riskLevel(riskLevel)
                .checkedAt(LocalDateTime.now())
                .build();

    }

    private static boolean isWhitelisted(Transaction txn) {
        List<String> whitelistedMerchants = List.of(
                "BESCOM", "LIC", "BSNL", "AIRTEL", "TATA_POWER"
        );
        return whitelistedMerchants.contains(txn.getMerchantId());
    }

    private static boolean checkHighValue(Transaction txn) {
        return txn.getAmount().compareTo(BigDecimal.valueOf(500000)) > 0;
    }

    private static boolean checkVelocity(Transaction txn) {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            long recentTransactions = transactionRepository.countByCustomerIdSince(
                    txn.getCustomerId(),
                    fiveMinutesAgo
            );
            return recentTransactions > 5;
        }


    private static boolean checkUnusualHour(Transaction txn) {
        int hour = txn.getInitiatedAt().getHour();
        return hour >= 0 && hour < 5;
    }
}