//the valid is used for validation for service controller 
package com.paymentplatform.payment_service.controller;

import com.paymentplatform.payment_service.dto.PaymentRequest;
import com.paymentplatform.payment_service.dto.PaymentResponse;
import com.paymentplatform.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        log.info("Payment initiation request for merchant: {}", request.getMerchantId());
        PaymentResponse response = paymentService.initiatePayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponse> getTransaction(@PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.getTransaction(transactionId));
    }
}
