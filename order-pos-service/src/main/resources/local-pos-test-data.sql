-- Run manually against LOCAL db_order only, after Hibernate creates the tables.
-- Test fixtures are never loaded automatically in production.
SET NAMES utf8mb4;
INSERT INTO vouchers (code,type,amount,minimum_amount,maximum_discount,active,members_only,stack_points,starts_at,expires_at,usage_limit,per_customer_limit,used_count)
VALUES ('POS10','PERCENT',10,500000,100000,1,0,1,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 30 DAY,100,NULL,0),
('POS50K','FIXED',50000,500000,50000,1,0,1,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 30 DAY,100,NULL,0),
('EXPIRED','FIXED',50000,0,50000,1,0,1,NOW()-INTERVAL 30 DAY,NOW()-INTERVAL 1 DAY,100,NULL,0)
ON DUPLICATE KEY UPDATE code=VALUES(code);
-- Customer 10 is the existing local customer fixture. Do not overwrite an existing balance.
INSERT IGNORE INTO loyalty_accounts (customer_id,balance,version) VALUES (10,100,0);
