package com.paymentplatform.payment_service.service;

import com.paymentplatform.payment_service.dto.GatewayResponse;

import com.paymentplatform.payment_service.dto.PaymentRequest;
import com.paymentplatform.payment_service.dto.PaymentResponse;
import com.paymentplatform.payment_service.entity.Transaction;
import com.paymentplatform.payment_service.enums.TransactionStatus;
import com.paymentplatform.payment_service.exception.GatewayTimeoutException;
import com.paymentplatform.payment_service.exception.TransactionNotFoundException;
import com.paymentplatform.payment_service.repository.TransactionRepository;
import com.paymentplatform.payment_service.simulator.GatewaySimulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final GatewaySimulator gatewaySimulator;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey) {

        // 1. Idempotency check
        var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate request detected for idempotency key: {}", idempotencyKey);
            return PaymentResponse.fromTransaction(existing.get());
        }

        // 2. Build and save transaction
        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .merchantId(request.getMerchantId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.INITIATED)
                .idempotencyKey(idempotencyKey)
                .initiatedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(txn);
        log.info("Transaction created: {}", txn.getId());

        // 3. Move to gateway pending
        txn.setStatus(TransactionStatus.GATEWAY_PENDING);
        txn.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(txn); try {
            GatewayResponse gatewayResponse = gatewaySimulator.process(txn);

            if (gatewayResponse.isSuccess()) {
                // Happy path — payment went through
                txn.setStatus(TransactionStatus.GATEWAY_SUCCESS);
                txn.setGatewayTransactionId(gatewayResponse.getGatewayTransactionId());
            } else {
                // Gateway rejected the payment (insufficient funds, card declined)
                txn.setStatus(TransactionStatus.GATEWAY_FAILED);
                txn.setFailureReason(gatewayResponse.getFailureReason());
            }

        } catch (GatewayTimeoutException e) {
            // Timeout — we don't know if payment went through!
            // Keep as GATEWAY_PENDING — reconciliation engine will resolve this later
            log.error("Gateway timeout for txn: {} — marked for reconciliation",
                    txn.getId());
            txn.setFailureReason("GATEWAY_TIMEOUT - pending reconciliation");
        }



        return PaymentResponse.fromTransaction(txn);
    }

    public PaymentResponse getTransaction(String transactionId) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return PaymentResponse.fromTransaction(txn);
    }
}