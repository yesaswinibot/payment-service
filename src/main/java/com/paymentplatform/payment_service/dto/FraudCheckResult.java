package com.paymentplatform.payment_service.dto;

import com.paymentplatform.payment_service.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResult{
    private String transactionId;
    private boolean isFraud;
    private List<String> reasons;
    private List<String> triggeredRules;
    private RiskLevel riskLevel;
    private LocalDateTime checkedAt;
}