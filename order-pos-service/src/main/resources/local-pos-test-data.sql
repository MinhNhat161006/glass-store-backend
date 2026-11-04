-- Run manually against LOCAL db_order only, after Hibernate creates the tables.
-- Test fixtures are never loaded automatically in production.
SET NAMES utf8mb4;
INSERT INTO vouchers (code,name,discount_type,discount_value,min_order_amount,max_discount_amount,active,members_only,stack_points,valid_from,valid_to,usage_limit,per_customer_limit,used_count,channel)
VALUES ('POS10','POS 10%','PERCENT',10,500000,100000,1,0,1,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 30 DAY,100,NULL,0,'POS'),
('POS50K','POS 50K','FIXED',50000,500000,50000,1,0,1,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 30 DAY,100,NULL,0,'POS'),
('EXPIRED','Expired test','FIXED',50000,0,50000,1,0,1,NOW()-INTERVAL 30 DAY,NOW()-INTERVAL 1 DAY,100,NULL,0,'POS')
ON DUPLICATE KEY UPDATE code=VALUES(code);
-- Customer 10 is the existing local customer fixture. Do not overwrite an existing balance.
INSERT IGNORE INTO loyalty_accounts (customer_id,balance,version) VALUES (10,100,0);
