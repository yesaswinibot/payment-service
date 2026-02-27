package com.paymentplatform.payment_service.simulator;

import com.paymentplatform.payment_service.dto.GatewayResponse;
import com.paymentplatform.payment_service.entity.Transaction;
import com.paymentplatform.payment_service.exception.GatewayTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Component
@Slf4j
public class GatewaySimulator {

    private final Random random = new Random();

    public GatewayResponse process(Transaction transaction) {

        log.info("Sending transaction {} to gateway...", transaction.getId());

        // Simulate real network delay (100ms - 500ms)
        simulateNetworkDelay();

        // Generate random outcome mimicking real gateway behavior
        double outcome = random.nextDouble(); // 0.0 to 1.0

        if (outcome < 0.75) {
            // 75% success rate — realistic for a healthy payment system
            String gatewayTxnId = "GW-" + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
            log.info("Gateway SUCCESS for txn: {} → gatewayId: {}",
                    transaction.getId(), gatewayTxnId);
            return GatewayResponse.success(gatewayTxnId);

        } else if (outcome < 0.88) {
            // 13% insufficient funds
            log.warn("Gateway INSUFFICIENT_FUNDS for txn: {}", transaction.getId());
            return GatewayResponse.failed(GatewayResponse.GatewayStatus.INSUFFICIENT_FUNDS);

        } else if (outcome < 0.95) {
            // 7% card declined by bank
            log.warn("Gateway CARD_DECLINED for txn: {}", transaction.getId());
            return GatewayResponse.failed(GatewayResponse.GatewayStatus.CARD_DECLINED);

        } else {
            // 5% timeout — THE DANGEROUS CASE
            // Gateway may have processed payment but we never got confirmation
            // This is what causes reconciliation mismatches!
            log.error("Gateway TIMEOUT for txn: {} — reconciliation needed!",
                    transaction.getId());
            throw new GatewayTimeoutException(transaction.getId());
        }
    }

    private void simulateNetworkDelay() {
        try {
            int delay = 100 + random.nextInt(400); // 100-500ms
            log.debug("Simulating gateway delay: {}ms", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/*0.00 → 0.75  =  75%  SUCCESS
0.75 → 0.88  =  13%  INSUFFICIENT_FUNDS
0.88 → 0.95  =   7%  CARD_DECLINED
0.95 → 1.00  =   5%  TIMEOUT*/