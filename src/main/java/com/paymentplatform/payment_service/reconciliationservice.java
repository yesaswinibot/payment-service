package com.paymentplatform.payment_service;

import com.paymentplatform.payment_service.entity.Transaction;
import com.paymentplatform.payment_service.enums.TransactionStatus;
import com.paymentplatform.payment_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class reconciliationservice {

    private final TransactionRepository transactionRepository;
    @Scheduled(fixedDelay = 300000)

    public void reconcile() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        List<Transaction> stuckTransactions = transactionRepository.findStuckTransactions(
                List.of(TransactionStatus.GATEWAY_PENDING,
                        TransactionStatus.SETTLEMENT_PENDING),
                fiveMinutesAgo
        );

        log.info("Found {} stuck transactions for reconciliation",
                stuckTransactions.size());

        for (Transaction txn : stuckTransactions) {
            if (isDiscrepancy(txn)) {
                reconcileTransaction(txn);
            }
        }
    }

    private void reconcileTransaction(Transaction txn) {
        log.warn("Reconciling stuck transaction: {} status: {}",
                txn.getId(), txn.getStatus());
        txn.setStatus(TransactionStatus.RECONCILIATION_FAILED);
        txn.setFailureReason("Flagged by reconciliation - requires manual review");
        txn.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(txn);
        log.info("Transaction {} flagged for manual review", txn.getId());
    }

    private boolean isDiscrepancy(Transaction txn) {
        return txn.getStatus() == TransactionStatus.GATEWAY_PENDING ||
                txn.getStatus() == TransactionStatus.SETTLEMENT_PENDING;
    }

}



