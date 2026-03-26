package com.paymentplatform.payment_service.repository;

import com.paymentplatform.payment_service.entity.Transaction;
import com.paymentplatform.payment_service.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // Find by idempotency key — used for duplicate detection
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // Find all transactions for a merchant
    List<Transaction> findByMerchantIdOrderByInitiatedAtDesc(String merchantId);

    // Find all transactions by status
    List<Transaction> findByStatusOrderByInitiatedAtDesc(TransactionStatus status);

    // Count transactions by merchant and status
    long countByMerchantIdAndStatus(String merchantId, TransactionStatus status);

    // Sum of successful transaction amounts for a merchant
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.merchantId = :merchantId " +
            "AND t.status = :status")
    BigDecimal sumAmountByMerchantIdAndStatus(
            @Param("merchantId") String merchantId,
            @Param("status") TransactionStatus status);

    // Total transaction count for a merchant
    long countByMerchantId(String merchantId);
    @Query("SELECT COUNT(t) FROM Transaction t " +
            "WHERE t.customerId = :customerId " +
            "AND t.initiatedAt >= :since")
    long countByCustomerIdSince(
            @Param("customerId") String customerId,
            @Param("since") LocalDateTime since);
    @Query("SELECT t FROM Transaction t WHERE t.status IN :statuses AND t.initiatedAt <= :before")
    List<Transaction> findStuckTransactions(
            @Param("statuses") List<TransactionStatus> statuses,
            @Param("before") LocalDateTime before
    );
}