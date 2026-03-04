package com.paymentplatform.payment_service.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import com.paymentplatform.payment_service.enums.RiskLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResult {
    private String transactionId;
    private boolean isFraud;
    private List<String> reasons;
    private List<String> triggeredRules;
    private RiskLevel riskLevel;
    private LocalDateTime checkedAt;
}