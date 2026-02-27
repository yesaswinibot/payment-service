package com.paymentplatform.payment_service.exception;

public class GatewayTimeoutException extends RuntimeException {

    public GatewayTimeoutException(String transactionId) {
        super("Gateway timeout for transaction: " + transactionId);
    }
}