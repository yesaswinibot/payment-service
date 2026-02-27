package com.paymentplatform.payment_service.enums;

public enum TransactionStatus {
    INITIATED,
    FRAUD_CHECK_PENDING,
    FRAUD_REJECTED,
    GATEWAY_PENDING,
    GATEWAY_SUCCESS,
    GATEWAY_FAILED,
    SETTLEMENT_PENDING,
    SETTLED,
    RECONCILIATION_FAILED
}
