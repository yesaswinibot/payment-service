package com.paymentplatform.payment_service.service;

import com.paymentplatform.payment_service.dto.*;
import com.paymentplatform.payment_service.entity.Transaction;
import com.paymentplatform.payment_service.enums.TransactionStatus;
import com.paymentplatform.payment_service.exception.GatewayTimeoutException;
import com.paymentplatform.payment_service.exception.TransactionNotFoundException;
import com.paymentplatform.payment_service.repository.TransactionRepository;
import com.paymentplatform.payment_service.simulator.GatewaySimulator;
import com.paymentplatform.payment_service.StateMachine.PaymentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final GatewaySimulator gatewaySimulator;
    private final FraudCheckService fraudCheckService;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request,
                                           String idempotencyKey) {

        // 1. Idempotency check
        var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate request for key: {}", idempotencyKey);
            return PaymentResponse.fromTransaction(existing.get());
        }

        // 2. Create transaction as INITIATED
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

        // 3. INITIATED → FRAUD_CHECK_PENDING
        transition(txn, TransactionStatus.FRAUD_CHECK_PENDING);

        // 4. Real fraud check
        FraudCheckResult fraudResult = fraudCheckService.check(txn);
        if (fraudResult.isFraud()) {
            transition(txn, TransactionStatus.FRAUD_REJECTED);
            txn.setFailureReason(String.join(", ", fraudResult.getReasons()));
            transactionRepository.save(txn);
            return PaymentResponse.fromTransaction(txn);
        }

        // 5. FRAUD_CHECK_PENDING → GATEWAY_PENDING
        transition(txn, TransactionStatus.GATEWAY_PENDING);

        // 6. Send to gateway
        try {
            GatewayResponse gatewayResponse = gatewaySimulator.process(txn);

            if (gatewayResponse.isSuccess()) {
                transition(txn, TransactionStatus.GATEWAY_SUCCESS);
                txn.setGatewayTransactionId(
                        gatewayResponse.getGatewayTransactionId()
                );
                transition(txn, TransactionStatus.SETTLEMENT_PENDING);

            } else {
                transition(txn, TransactionStatus.GATEWAY_FAILED);
                txn.setFailureReason(gatewayResponse.getFailureReason());
            }

        } catch (GatewayTimeoutException e) {
            log.error("Gateway timeout for txn: {}", txn.getId());
            txn.setFailureReason("GATEWAY_TIMEOUT - pending reconciliation");
        }

        // 7. Save final state
        txn.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(txn);

        return PaymentResponse.fromTransaction(txn);
    }

    public PaymentResponse getTransaction(String transactionId) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return PaymentResponse.fromTransaction(txn);
    }

    public List<PaymentResponse> getMerchantTransactions(String merchantId) {
        List<Transaction> transactions = transactionRepository
                .findByMerchantIdOrderByInitiatedAtDesc(merchantId);
        if (transactions.isEmpty()) {
            throw new TransactionNotFoundException(
                    "No transactions found for merchant: " + merchantId
            );
        }
        return transactions.stream()
                .map(PaymentResponse::fromTransaction)
                .collect(java.util.stream.Collectors.toList());
    }

    public MerchantSummaryResponse getMerchantSummary(String merchantId) {
        long total = transactionRepository.countByMerchantId(merchantId);
        if (total == 0) {
            throw new TransactionNotFoundException(
                    "No transactions found for merchant: " + merchantId
            );
        }
        long successful = transactionRepository.countByMerchantIdAndStatus(
                merchantId, TransactionStatus.GATEWAY_SUCCESS);
        long failed = transactionRepository.countByMerchantIdAndStatus(
                merchantId, TransactionStatus.GATEWAY_FAILED);
        long pending = transactionRepository.countByMerchantIdAndStatus(
                merchantId, TransactionStatus.GATEWAY_PENDING);
        BigDecimal totalVolume = transactionRepository.sumAmountByMerchantIdAndStatus(
                merchantId, TransactionStatus.GATEWAY_SUCCESS);
        return MerchantSummaryResponse.builder()
                .merchantId(merchantId)
                .totalTransactions(total)
                .successfulTransactions(successful)
                .failedTransactions(failed)
                .pendingTransactions(pending)
                .totalVolume(totalVolume)
                .successRate(MerchantSummaryResponse
                        .calculateSuccessRate(successful, total))
                .build();
    }

    public List<PaymentResponse> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository
                .findByStatusOrderByInitiatedAtDesc(status)
                .stream()
                .map(PaymentResponse::fromTransaction)
                .collect(java.util.stream.Collectors.toList());
    }

    private void transition(Transaction txn, TransactionStatus newStatus) {
        PaymentStateMachine.validateTransition(txn.getStatus(), newStatus);
        log.info("Transaction {} : {} → {}",
                txn.getId(), txn.getStatus(), newStatus);
        txn.setStatus(newStatus);
        txn.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(txn);
    }
}