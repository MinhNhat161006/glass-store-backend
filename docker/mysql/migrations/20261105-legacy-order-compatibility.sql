-- Upgrade pre-Sprint-2 payment rows without resetting orders or payments.
USE db_order;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'payments' AND column_name = 'created_at'),
    'SELECT 1', 'ALTER TABLE payments ADD COLUMN created_at DATETIME(6) NULL');
PREPARE upgrade_statement FROM @ddl;
EXECUTE upgrade_statement;
DEALLOCATE PREPARE upgrade_statement;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'payments' AND column_name = 'updated_at'),
    'SELECT 1', 'ALTER TABLE payments ADD COLUMN updated_at DATETIME(6) NULL');
PREPARE upgrade_statement FROM @ddl;
EXECUTE upgrade_statement;
DEALLOCATE PREPARE upgrade_statement;

-- Prefer existing purchase/payment dates; migration time is the final fallback
-- only when the legacy database contains no usable timestamp.
UPDATE payments p LEFT JOIN orders o ON o.id = p.order_id
SET p.created_at = COALESCE(p.created_at, p.paid_at, o.created_at, CURRENT_TIMESTAMP(6)),
    p.updated_at = COALESCE(p.updated_at, p.paid_at, o.updated_at, o.created_at, CURRENT_TIMESTAMP(6))
WHERE p.created_at IS NULL OR p.updated_at IS NULL;
ALTER TABLE payments
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Same lifecycle state under its canonical name; preserve modification time.
UPDATE orders SET status = 'PREPARING', updated_at = updated_at WHERE status = 'PROCESSING';
