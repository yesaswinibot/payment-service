package com.paymentplatform.payment_service.dto;
import com.paymentplatform.payment_service.entity.Transaction;
import com.paymentplatform.payment_service.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private String transactionId;
    private String merchantId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private String paymentMethod;
    private String message;
    private LocalDateTime initiatedAt;

    public static PaymentResponse fromTransaction(Transaction txn) {
        return PaymentResponse.builder()
                .transactionId(txn.getId())
                .merchantId(txn.getMerchantId())
                .customerId(txn.getCustomerId())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .status(txn.getStatus())
                .paymentMethod(txn.getPaymentMethod().name())
                .message(resolveMessage(txn.getStatus()))
                .initiatedAt(txn.getInitiatedAt())
                .build();
    }

    private static String resolveMessage(TransactionStatus status) {
        return switch (status) {
            case INITIATED -> "Payment initiated";
            case GATEWAY_PENDING -> "Processing with gateway";
            case GATEWAY_SUCCESS -> "Payment successful";
            case GATEWAY_FAILED -> "Payment failed at gateway";
            case FRAUD_REJECTED -> "Transaction rejected due to risk";
            case SETTLEMENT_PENDING -> "Awaiting settlement";
            case SETTLED -> "Payment settled";
            default -> "Processing";
        };
    }
}


