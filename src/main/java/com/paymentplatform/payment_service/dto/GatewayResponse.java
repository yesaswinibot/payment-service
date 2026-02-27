package com.paymentplatform.payment_service.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GatewayResponse {

    private boolean success;
    private String gatewayTransactionId;
    private String failureReason;
    private GatewayStatus status;

    public enum GatewayStatus {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        CARD_DECLINED,
        TIMEOUT,
        UNKNOWN_ERROR
    }

    // ── Static factory methods — clean way to create responses ──

    public static GatewayResponse success(String gatewayTxnId) {
        return GatewayResponse.builder()
                .success(true)
                .gatewayTransactionId(gatewayTxnId)
                .status(GatewayStatus.SUCCESS)
                .build();
    }

    public static GatewayResponse failed(GatewayStatus reason) {
        return GatewayResponse.builder()
                .success(false)
                .status(reason)
                .failureReason(reason.name())
                .build();
    }
}