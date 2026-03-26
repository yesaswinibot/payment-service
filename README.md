# Payment Processing & Fraud Detection Microservice

A cloud-native payment processing system built with Spring Boot that handles 
real-world banking challenges like fraud detection, transaction reconciliation, 
and payment state management.

## Problem It Solves
In payment systems, a payment gateway can succeed but internal database 
processing can fail — creating mismatches where customers are charged but 
the system has no record. This service detects and resolves these mismatches 
through automated reconciliation.

## Features
- **Payment Processing** — End to end transaction lifecycle management
- **Fraud Detection** — Multi-rule fraud engine with risk scoring
  - High value transaction flagging
  - Velocity checks (multiple transactions in short time)
  - Unusual hour detection
  - Merchant whitelisting
- **State Machine** — Ensures transactions only move in valid directions
- **Idempotency** — Prevents duplicate payment processing
- **Merchant Analytics** — Transaction summaries and success rates
- **Reconciliation** — *(In Progress)* Detects gateway vs internal mismatches

## Tech Stack
- Java 17
- Spring Boot
- MySQL
- Spring Data JPA
- Lombok
- Maven

## API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/payments/initiate | Initiate a payment |
| GET | /api/v1/payments/{id} | Get transaction by ID |
| GET | /api/v1/payments/merchant/{id} | Get merchant transactions |
| GET | /api/v1/payments/merchant/{id}/summary | Get merchant summary |
| GET | /api/v1/payments/status/{status} | Get transactions by status |

## Transaction Flow
```
INITIATED → FRAUD_CHECK_PENDING → FRAUD_REJECTED
                                → GATEWAY_PENDING → GATEWAY_FAILED
                                                  → GATEWAY_SUCCESS → SETTLEMENT_PENDING
```

## What I Learned
- Designing state machines for financial transaction management
- Idempotency patterns to prevent duplicate charges
- Multi-rule fraud detection with aggregated risk scoring
- Why payment reconciliation matters in real banking systems
