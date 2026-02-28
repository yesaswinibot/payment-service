package com.paymentplatform.payment_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
public class MerchantSummaryResponse {

    private String merchantId;
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private long pendingTransactions;
    private BigDecimal totalVolume;
    private BigDecimal successRate;

    // Calculate success rate as percentage
    public static BigDecimal calculateSuccessRate(long successful, long total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(successful)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}