# Purchase & Order Domain — Shared Implementation Context

Compact shared contract for all agents working on the purchasing/order backend.
Read this instead of re-reading the whole repository.

## 1. Existing architecture (do not replace)

| Module | Port | DB | Role |
|---|---|---|---|
| `api-gateway` | 8080 | — | Routes auth/users/products/orders/cart/pos/payments/vouchers/lab. No JWT filter. |
| `identity-service` | 8081 | H2 `db_identity` | Login/User CRUD. |
| `catalog-service` | 8082 | H2 `db_catalog` | Products + stock reserve/release/commit/adjust. |
| `order-pos-service` | 8083 | H2 `db_order` | Cart, checkout, POS, payment, voucher, order lifecycle. |
| `eyewear-lab-service` | 8084 | H2 `db_lab` | Prescription, lab job, QC history, warranty. |

- Java 25, Spring Boot 3.4.3, Spring Cloud 2024.0.0, JPA + Hibernate, Lombok.
- Persistence: `ddl-auto: update` on H2 in MySQL mode (no Flyway).
- Response envelope: `ApiResponse<T>` = `{code, message, data, timestamp}`.
- Portal `GET /api/orders` still returns a bare array.

## 2. Shared enums

`SalesChannel`: `ONLINE`, `POS`
`PaymentMethod`: `CASH`, `VIETQR`, `VNPAY`, `CARD`, `COD`
`PaymentStatus`: `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `REFUND_PENDING`, `REFUNDED`
`OrderStatus`: `PENDING_PAYMENT`, `PAID`, `PENDING_CONFIRMATION`, `CONFIRMED`, `PREPARING`,
`SHIPPING`, `READY_FOR_PICKUP`, `COMPLETED`, `CANCEL_REQUESTED`, `CANCELLED`,
`REFUND_PENDING`, `REFUNDED`
`LabJobStatus`: `WAITING_PROCESS`, `RECEIVED`, `IN_PROGRESS`, `QC_CHECK`, `QC_PASSED`,
`QC_FAILED`, `READY_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`

Legacy reads: order `PROCESSING` → `PREPARING`. Lab aliases `WAITING`, `READY_FOR_HANDOVER`.

## 3. Business rules implemented

- `BR-05`/`BR-06` Item `unitPrice`/`productName` are snapshots from catalog at confirmation.
- `BR-04` Discontinued / inactive SKUs cannot enter cart, checkout, or POS.
- `BR-08`/`BR-09` Available = actual − reserved − non-sellable. Never negative; row lock on mutate.
- `BR-13` Unique `orderCode` (`ORD-yyyyMMdd-####`).
- `BR-14` Checkout validates customer, SKU, stock, prescription when required.
- `BR-15` At most one `SUCCESS` payment per order.
- `BR-16` Paid only after cash staff action or verified gateway callback. Never from client.
- `BR-18` Paid cancel → `REFUND_PENDING` on order **and** payment (no gateway refund executed).
- `BR-19`/`BR-20` Voucher uses stored window, min amount, type, value, max cap. No stacking.
- `BR-29`..`BR-34` One lab job per order; QC reason required; handover only after `QC_PASSED`.
- Ownership: `GET /api/orders/{id}` with header `X-Customer-Id` rejects other customers' orders.
  Full JWT RBAC is **not** wired in order-pos-service (identity owns login).

## 4. Money

`BigDecimal`, scale 2, `HALF_UP`.

## 5. Cross-service contract (Feign)

order-pos → catalog:

```
GET  /api/variants/sku/{sku}
GET  /api/variants/barcode/{barcode}
GET  /api/variants/search?q=
POST /api/variants/reserve
POST /api/variants/release
POST /api/variants/commit
POST /api/variants/adjust
```

order-pos → lab:

```
POST /api/lab-jobs/from-order
```

Fail-fast Feign; never silent success.

## 6. Idempotency

- Checkout: header `Idempotency-Key`, unique `orders.idempotency_key`. Replay → HTTP 200.
- Stock: same `reservationKey` for all lines of an order; unique `(reservation_key, sku)`.
- Payment callback: unique `(provider, external_transaction_id)`.

## 7. Main public APIs (via gateway)

| Method | Path | Purpose |
|---|---|---|
| GET/POST/PATCH/DELETE | `/api/cart`, `/api/cart/items`, `/api/cart/checkout` | Customer cart |
| POST | `/api/orders` | Direct checkout (also used by portal) |
| GET | `/api/orders`, `/api/orders/{id}`, `/api/orders/customer/{id}` | History |
| POST | `/api/orders/{id}/cancel-request`, `/cancel`, `/settle-cash` | Cancel / cash |
| POST | `/api/pos/sessions`, `/api/pos/checkout`, `/api/pos/preview` | POS |
| GET | `/api/pos/sku/{sku}`, `/barcode/{barcode}`, `/search`, `/sales` | POS lookup |
| POST | `/api/payments/order/{id}/initiate`, `/callback`, `/refund-request` | Payment |
| GET | `/api/vouchers/{code}/quote` | Voucher preview |
| POST | `/api/lab-jobs/from-order`, PATCH `/api/lab-jobs/{id}/status` | Fabrication |

## 8. Known deviations (not invented)

- No payment gateway selected. Production bean: `UnconfiguredPaymentGateway`.
- No Flyway; `ddl-auto: update`.
- No reservation expiry (OQ-03).
- Voucher table exists but is **not seeded** with invented campaigns.
- JWT is not enforced on order/cart/POS endpoints; `X-Customer-Id` is a temporary ownership check.
