package com.paymentplatform.payment_service.StateMachine;
import com.paymentplatform.payment_service.enums.TransactionStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class PaymentStateMachine {

    // Define which transitions are ALLOWED
    // Key   = current status
    // Value = set of statuses it CAN move to
    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS
            = new EnumMap<>(TransactionStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
                TransactionStatus.INITIATED,
                EnumSet.of(TransactionStatus.FRAUD_CHECK_PENDING)
        );
        ALLOWED_TRANSITIONS.put(
                TransactionStatus.FRAUD_CHECK_PENDING,
                EnumSet.of(
                        TransactionStatus.FRAUD_REJECTED,
                        TransactionStatus.GATEWAY_PENDING
                )
        );
        ALLOWED_TRANSITIONS.put(
                TransactionStatus.GATEWAY_PENDING,
                EnumSet.of(
                        TransactionStatus.GATEWAY_SUCCESS,
                        TransactionStatus.GATEWAY_FAILED,
                        TransactionStatus.RECONCILIATION_FAILED
                )
        );
        ALLOWED_TRANSITIONS.put(
                TransactionStatus.GATEWAY_SUCCESS,
                EnumSet.of(TransactionStatus.SETTLEMENT_PENDING)
        );
        ALLOWED_TRANSITIONS.put(
                TransactionStatus.SETTLEMENT_PENDING,
                EnumSet.of(TransactionStatus.SETTLED)
        );

        // Terminal states — no transitions allowed
        ALLOWED_TRANSITIONS.put(
                TransactionStatus.FRAUD_REJECTED,
                EnumSet.noneOf(TransactionStatus.class)
        );
        ALLOWED_TRANSITIONS.put(
                TransactionStatus.GATEWAY_FAILED,
                EnumSet.noneOf(TransactionStatus.class)
        );
        ALLOWED_TRANSITIONS.put(
                TransactionStatus.SETTLED,
                EnumSet.noneOf(TransactionStatus.class)
        );
        ALLOWED_TRANSITIONS.put(
                TransactionStatus.RECONCILIATION_FAILED,
                EnumSet.noneOf(TransactionStatus.class)
        );
    }

    // Check if transition is valid
    public static boolean isValidTransition(TransactionStatus from,
                                            TransactionStatus to) {
        Set<TransactionStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    // Validate and throw if invalid
    public static void validateTransition(TransactionStatus from,
                                          TransactionStatus to) {
        if (!isValidTransition(from, to)) {
            throw new IllegalStateException(
                    String.format("Invalid transition: %s → %s", from, to)
            );
        }
    }
}