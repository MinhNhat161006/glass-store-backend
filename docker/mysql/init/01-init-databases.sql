-- Script khởi tạo tự động 4 Schema độc lập cho 4 Microservices
-- Tương thích 100% với kiến trúc Logical Database Separation

CREATE DATABASE IF NOT EXISTS `db_identity` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `db_catalog`  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `db_order`    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `db_lab`      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
