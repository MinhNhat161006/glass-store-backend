# Order POS — local development

The default profile follows the team's in-memory H2 configuration. Use the `local`
profile to persist POS orders and payments in Docker MySQL `db_order` (port 3307).

From the backend root, start `docker compose up -d`, then build:

```powershell
mvn -pl order-pos-service -am install -DskipTests
mvn -pl order-pos-service package spring-boot:repackage -DskipTests
java -jar order-pos-service/target/order-pos-service-1.0.0.jar --spring.profiles.active=local
```

Override `ORDER_DB_URL`, `ORDER_DB_USERNAME`, and `ORDER_DB_PASSWORD` in the local
environment as needed. Hibernate's existing `ddl-auto: update` adds `tax_amount`
and updates the payment method column to accept external card-terminal sales.
Never remove the Docker volume when restarting an existing database.

`POST /api/orders` returns HTTP 201 and the persisted order. POS totals are
recalculated from item quantities and unit prices, less discount plus the existing
8% VAT rule. Items and payment are saved in one transaction. Invalid or empty
orders return HTTP 400. Prices are supplied by the POS's live catalog; server-side
catalog price verification and inventory deduction are separate work.

Cashiers confirm receipt of cash, bank transfer or external card-terminal payment.
This endpoint records that confirmation; it does not charge a bank card or verify
a bank transfer automatically. No fiscal e-invoice is issued by the K80 receipt.
