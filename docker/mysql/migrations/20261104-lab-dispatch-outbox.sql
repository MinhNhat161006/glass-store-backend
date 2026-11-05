-- Run against db_order before deploying the POS -> Lab dispatch worker.
USE db_order;
CREATE TABLE IF NOT EXISTS lab_dispatch_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    lab_job_id BIGINT NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    CONSTRAINT uk_lab_dispatch_order UNIQUE (order_id),
    INDEX idx_lab_dispatch_pending (status, created_at)
);
