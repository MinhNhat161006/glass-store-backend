-- ==============================================================================
-- 02-seed-data.sql: Khởi tạo bảng dữ liệu và nạp dữ liệu mẫu (Seed Data)
-- Chuẩn hóa theo SRS Omnichannel Eyewear Retail Management System
-- ==============================================================================

-- ==============================================================================
-- 1. SCHEMA: db_identity (Xác thực, Người dùng & Phân quyền RBAC)
-- ==============================================================================
USE `db_identity`;

CREATE TABLE IF NOT EXISTS `roles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `phone_number` VARCHAR(20),
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nạp Roles
INSERT INTO `roles` (`id`, `name`, `description`) VALUES
(1, 'ROLE_ADMIN', 'Quản trị viên toàn hệ thống'),
(2, 'ROLE_SALES_STAFF', 'Nhân viên bán hàng tại quầy POS'),
(3, 'ROLE_OPTOMETRIST', 'Kỹ thuật viên đo mắt khúc xạ'),
(4, 'ROLE_LAB_TECH', 'Kỹ thuật viên xưởng gia công mài lắp kính'),
(5, 'ROLE_CUSTOMER', 'Khách hàng mua sắm online')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- Nạp Users mẫu (Mật khẩu mặc định: 123456 -> mã hóa BCrypt: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi)
INSERT INTO `users` (`id`, `username`, `email`, `password_hash`, `full_name`, `phone_number`, `status`) VALUES
(1, 'admin', 'admin@glassstore.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Quản Trị Viên', '0901000001', 'ACTIVE'),
(2, 'staff', 'staff@glassstore.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Nguyễn Bán Hàng (POS)', '0901000002', 'ACTIVE'),
(3, 'optometrist', 'optometrist@glassstore.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Bác Sĩ Trần Đo Mắt', '0901000003', 'ACTIVE'),
(4, 'labtech', 'lab@glassstore.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Lê Kỹ Thuật Xưởng Mài', '0901000004', 'ACTIVE'),
(5, 'customer', 'customer@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Phạm Khách Hàng', '0987654321', 'ACTIVE')
ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`);

-- Gán quyền cho User
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);


-- ==============================================================================
-- 2. SCHEMA: db_catalog (Danh mục, Sản phẩm mắt kính & Tồn kho)
-- ==============================================================================
USE `db_catalog`;

CREATE TABLE IF NOT EXISTS `categories` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `slug` VARCHAR(100) NOT NULL UNIQUE,
    `description` VARCHAR(255),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `brands` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `country` VARCHAR(50),
    `description` VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `products` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `slug` VARCHAR(255) NOT NULL UNIQUE,
    `category_id` BIGINT NOT NULL,
    `brand_id` BIGINT NOT NULL,
    `product_type` ENUM('FRAME', 'LENS', 'SUNGLASSES', 'ACCESSORY') NOT NULL,
    `description` TEXT,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
    CONSTRAINT `fk_products_brand` FOREIGN KEY (`brand_id`) REFERENCES `brands` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `product_variants` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `product_id` BIGINT NOT NULL,
    `sku` VARCHAR(100) NOT NULL UNIQUE,
    `barcode` VARCHAR(100) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `color` VARCHAR(50),
    `frame_shape` VARCHAR(50),     -- Mắt vuông, Mắt tròn, Aviator, Chữ nhật, Mắt mèo
    `material` VARCHAR(50),        -- Titanium, Nhựa TR90, Acetate, Hợp kim
    `lens_index` VARCHAR(20),      -- Chiết suất tròng: 1.56, 1.60, 1.67, 1.74
    `price` DECIMAL(12, 2) NOT NULL,
    `cost_price` DECIMAL(12, 2) NOT NULL,
    `stock_quantity` INT NOT NULL DEFAULT 50,
    `image_url` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_variants_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nạp Categories
INSERT INTO `categories` (`id`, `name`, `slug`, `description`) VALUES
(1, 'Gọng Kính', 'gong-kinh', 'Gọng kính cận, viễn, loạn thời trang cao cấp'),
(2, 'Tròng Kính', 'trong-kinh', 'Tròng kính thuốc chống ánh sáng xanh, chống trầy, đổi màu'),
(3, 'Kính Râm', 'kinh-ram', 'Kính mát thời trang chống tia UV'),
(4, 'Phụ Kiện', 'phu-kien', 'Hộp kính, khăn lau nano, nước lau kính')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- Nạp Brands
INSERT INTO `brands` (`id`, `name`, `country`, `description`) VALUES
(1, 'Bolon', 'Pháp', 'Thương hiệu gọng kính thời trang châu Âu'),
(2, 'Ray-Ban', 'Mỹ', 'Thương hiệu kính râm và gọng kính biểu tượng thế giới'),
(3, 'Gentle Monster', 'Hàn Quốc', 'Thiết kế gọng kính phá cách dẫn đầu xu hướng'),
(4, 'Chemi', 'Hàn Quốc', 'Tròng kính quang học chất lượng cao phổ biến nhất'),
(5, 'Essilor', 'Pháp', 'Tập đoàn tròng kính số 1 thế giới với công nghệ Crizal và Transitions')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- Nạp Products
INSERT INTO `products` (`id`, `name`, `slug`, `category_id`, `brand_id`, `product_type`, `description`, `status`) VALUES
(1, 'Gọng Kính Titanium Bolon Square', 'gong-kinh-titanium-bolon-square', 1, 1, 'FRAME', 'Gọng kính Titanium siêu nhẹ cao cấp, chống ăn mòn, thiết kế mắt vuông nam tính', 'ACTIVE'),
(2, 'Gọng Kính Nhựa Dẻo TR90 Gentle Monster South Side', 'gong-kinh-nhua-deo-tr90-gentle-monster', 1, 3, 'FRAME', 'Chất liệu TR90 siêu bền uốn dẻo, dáng mắt tròn vintage phong cách Hàn Quốc', 'ACTIVE'),
(3, 'Gọng Kính Aviator Ray-Ban Optical', 'gong-kinh-aviator-ray-ban-optical', 1, 2, 'FRAME', 'Dáng kính phi công huyền thoại thanh lịch, khung kim loại cao cấp mạ vàng/bạc', 'ACTIVE'),
(4, 'Tròng Kính Cận Chống Ánh Sáng Xanh Chemi U2 1.60', 'trong-kinh-can-chemi-u2-1-60', 2, 4, 'LENS', 'Chiết suất 1.60 mỏng nhẹ, lớp phủ Crystal U2 chống trầy xước, ngăn ánh sáng xanh màn hình', 'ACTIVE'),
(5, 'Tròng Kính Đổi Màu Essilor Transitions Gen 8 1.56', 'trong-kinh-doi-mau-essilor-transitions-gen-8', 2, 5, 'LENS', 'Công nghệ đổi màu siêu tốc khi ra nắng, bảo vệ mắt tối đa trước tia UV và ánh sáng xanh', 'ACTIVE'),
(6, 'Kính Râm Ray-Ban Classic Wayfarer Polarized', 'kinh-ram-ray-ban-classic-wayfarer', 3, 2, 'SUNGLASSES', 'Kính râm gọng đen mắt xanh đen phân cực chống lóa kinh điển', 'ACTIVE'),
(7, 'Hộp Đựng Kính Da Khâu Tay & Khăn Lau Nano', 'hop-dung-kinh-da-khan-lau-nano', 4, 1, 'ACCESSORY', 'Bộ phụ kiện bảo quản kính chuyên dụng cao cấp chống va đập trầy xước', 'ACTIVE')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- Nạp Biến thể sản phẩm (SKU) có tồn kho
INSERT INTO `product_variants` (`id`, `product_id`, `sku`, `barcode`, `name`, `color`, `frame_shape`, `material`, `lens_index`, `price`, `cost_price`, `stock_quantity`, `image_url`, `status`) VALUES
-- Bolon Square Titanium
(1, 1, 'FRM-BOLON-BLK', '89300100101', 'Gọng Bolon Titanium - Đen Nhám', 'Đen', 'Mắt vuông', 'Titanium', NULL, 3250000, 1800000, 45, 'https://images.unsplash.com/photo-1591076482161-42ce6da69f67?w=500', 'ACTIVE'),
(2, 1, 'FRM-BOLON-SIL', '89300100102', 'Gọng Bolon Titanium - Bạc Sang Trọng', 'Bạc', 'Mắt vuông', 'Titanium', NULL, 3250000, 1800000, 30, 'https://images.unsplash.com/photo-1574258495973-f010dfbb5371?w=500', 'ACTIVE'),

-- Gentle Monster TR90
(3, 2, 'FRM-GM-BLK', '89300100201', 'Gọng Gentle Monster TR90 - Đen Bóng', 'Đen', 'Mắt tròn', 'Nhựa TR90', NULL, 1850000, 950000, 50, 'https://images.unsplash.com/photo-1508296695146-257a814070b4?w=500', 'ACTIVE'),
(4, 2, 'FRM-GM-TOR', '89300100202', 'Gọng Gentle Monster TR90 - Đồi Mồi Nâu', 'Đồi mồi', 'Mắt tròn', 'Nhựa TR90', NULL, 1850000, 950000, 35, 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=500', 'ACTIVE'),

-- Ray-Ban Aviator Optical
(5, 3, 'FRM-RB-GLD', '89300100301', 'Gọng Ray-Ban Aviator - Mạ Vàng', 'Vàng kim', 'Aviator', 'Hợp kim', NULL, 4100000, 2400000, 25, 'https://images.unsplash.com/photo-1577803645773-f96470509666?w=500', 'ACTIVE'),

-- Tròng Chemi U2 1.60
(6, 4, 'LNS-CHEMI-160', '89300200101', 'Cặp Tròng Chemi U2 BlueCut 1.60', 'Trong suốt', NULL, 'Chất dẻo quang học', '1.60', 650000, 320000, 100, 'https://images.unsplash.com/photo-1582142839970-2b9da988941f?w=500', 'ACTIVE'),

-- Tròng Essilor Transitions 1.56
(7, 5, 'LNS-ESS-TRANS', '89300200201', 'Cặp Tròng Essilor Transitions Gen 8 1.56', 'Đổi màu khói', NULL, 'Chất dẻo quang học', '1.56', 3380000, 1950000, 60, 'https://images.unsplash.com/photo-1582142839970-2b9da988941f?w=500', 'ACTIVE'),

-- Kính râm Wayfarer
(8, 6, 'SUN-RB-WAYF-BLK', '89300300101', 'Kính Râm Ray-Ban Wayfarer Polarized', 'Đen bóng', 'Mắt vuông', 'Acetate', NULL, 4950000, 2900000, 20, 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=500', 'ACTIVE'),

-- Phụ kiện
(9, 7, 'ACC-BOX-NANO', '89300400101', 'Combo Hộp Đựng Da + Khăn Nano', 'Nâu bò', NULL, 'Da PU', NULL, 150000, 55000, 200, 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=500', 'ACTIVE')
ON DUPLICATE KEY UPDATE `price` = VALUES(`price`);


-- ==============================================================================
-- 3. SCHEMA: db_lab (Đo mắt khúc xạ OD/OS & Lệnh mài lắp gia công xưởng)
-- ==============================================================================
USE `db_lab`;

CREATE TABLE IF NOT EXISTS `prescriptions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `customer_id` BIGINT NOT NULL,
    `optometrist_id` BIGINT NOT NULL,
    `od_sph` DECIMAL(4, 2) NOT NULL, -- Mắt phải (OD) - Độ cầu
    `od_cyl` DECIMAL(4, 2) DEFAULT 0.00, -- Độ loạn
    `od_axis` INT DEFAULT 0,         -- Trục loạn (0 - 180 độ)
    `od_add` DECIMAL(4, 2) DEFAULT 0.00,  -- Độ cộng thêm (nhìn gần)
    `os_sph` DECIMAL(4, 2) NOT NULL, -- Mắt trái (OS) - Độ cầu
    `os_cyl` DECIMAL(4, 2) DEFAULT 0.00, -- Độ loạn
    `os_axis` INT DEFAULT 0,         -- Trục loạn (0 - 180 độ)
    `os_add` DECIMAL(4, 2) DEFAULT 0.00,
    `pd` DECIMAL(4, 1) NOT NULL,     -- Khoảng cách đồng tử (mm)
    `notes` VARCHAR(500),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `lab_jobs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `prescription_id` BIGINT NOT NULL,
    `frame_sku` VARCHAR(100) NOT NULL,
    `lens_sku` VARCHAR(100) NOT NULL,
    `technician_id` BIGINT,
    `status` ENUM('WAITING_PROCESS', 'IN_PROGRESS', 'QC_CHECK', 'QC_PASSED', 'QC_FAILED', 'READY_FOR_DELIVERY') DEFAULT 'WAITING_PROCESS',
    `qc_notes` VARCHAR(500),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_lab_jobs_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nạp 1 Phiếu đo mắt mẫu
INSERT INTO `prescriptions` (`id`, `customer_id`, `optometrist_id`, `od_sph`, `od_cyl`, `od_axis`, `od_add`, `os_sph`, `os_cyl`, `os_axis`, `os_add`, `pd`, `notes`) VALUES
(1, 5, 3, -2.50, -0.75, 175, 0.00, -3.00, -0.50, 180, 0.00, 62.5, 'Khách làm việc văn phòng máy tính nhiều, tư vấn tròng chống ánh sáng xanh Chemi 1.60')
ON DUPLICATE KEY UPDATE `od_sph` = VALUES(`od_sph`);

-- Nạp 1 Đơn gia công Lab mẫu
INSERT INTO `lab_jobs` (`id`, `order_id`, `prescription_id`, `frame_sku`, `lens_sku`, `technician_id`, `status`, `qc_notes`) VALUES
(1, 1001, 1, 'FRM-BOLON-BLK', 'LNS-CHEMI-160', 4, 'IN_PROGRESS', 'KTV đang mài lắp tròng vào gọng Bolon')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);


-- ==============================================================================
-- 4. SCHEMA: db_order (Giỏ hàng, Đơn online, Bán tại quầy POS & Thanh toán)
-- ==============================================================================
USE `db_order`;

CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_code` VARCHAR(50) NOT NULL UNIQUE,
    `customer_id` BIGINT,
    `customer_name` VARCHAR(100) NOT NULL,
    `customer_phone` VARCHAR(20) NOT NULL,
    `shipping_address` VARCHAR(255),
    `channel` ENUM('ONLINE', 'POS') DEFAULT 'ONLINE',
    `order_type` ENUM('FRAME_ONLY', 'LENS_ONLY', 'COMPLETE_GLASSES', 'ACCESSORY') DEFAULT 'COMPLETE_GLASSES',
    `total_amount` DECIMAL(12, 2) NOT NULL,
    `discount_amount` DECIMAL(12, 2) DEFAULT 0.00,
    `final_amount` DECIMAL(12, 2) NOT NULL,
    `status` ENUM('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPING', 'COMPLETED', 'CANCELLED') DEFAULT 'PAID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `order_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `sku_id` VARCHAR(100) NOT NULL, -- Logical reference sang db_catalog.product_variants.sku
    `product_name` VARCHAR(255) NOT NULL,
    `prescription_id` BIGINT,       -- Logical reference sang db_lab.prescriptions.id
    `quantity` INT NOT NULL DEFAULT 1,
    `unit_price` DECIMAL(12, 2) NOT NULL,
    `subtotal` DECIMAL(12, 2) NOT NULL,
    CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `payments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `payment_method` ENUM('CASH', 'VIETQR', 'VNPAY', 'COD') NOT NULL,
    `transaction_code` VARCHAR(100),
    `amount` DECIMAL(12, 2) NOT NULL,
    `status` ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'SUCCESS',
    `paid_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_payments_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nạp 1 Đơn hàng mẫu đã hoàn tất
INSERT INTO `orders` (`id`, `order_code`, `customer_id`, `customer_name`, `customer_phone`, `shipping_address`, `channel`, `order_type`, `total_amount`, `discount_amount`, `final_amount`, `status`) VALUES
(1001, 'ORD-20261030-001', 5, 'Phạm Khách Hàng', '0987654321', '123 Nguyễn Huệ, Quận 1, TP.HCM', 'ONLINE', 'COMPLETE_GLASSES', 3900000, 0, 3900000, 'PROCESSING')
ON DUPLICATE KEY UPDATE `final_amount` = VALUES(`final_amount`);

INSERT INTO `order_items` (`id`, `order_id`, `sku_id`, `product_name`, `prescription_id`, `quantity`, `unit_price`, `subtotal`) VALUES
(1, 1001, 'FRM-BOLON-BLK', 'Gọng Bolon Titanium - Đen Nhám', 1, 1, 3250000, 3250000),
(2, 1001, 'LNS-CHEMI-160', 'Cặp Tròng Chemi U2 BlueCut 1.60', 1, 1, 650000, 650000)
ON DUPLICATE KEY UPDATE `subtotal` = VALUES(`subtotal`);

INSERT INTO `payments` (`id`, `order_id`, `payment_method`, `transaction_code`, `amount`, `status`) VALUES
(1, 1001, 'VIETQR', 'VQR-MBB-987123', 3900000, 'SUCCESS')
ON DUPLICATE KEY UPDATE `amount` = VALUES(`amount`);

