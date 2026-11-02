package com.glassstore.catalog.config;

import com.glassstore.catalog.entity.*;
import com.glassstore.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogDataInitializer implements CommandLineRunner {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Catalog database đã có dữ liệu sản phẩm, bỏ qua nạp seed.");
            return;
        }

        log.info("Bắt đầu khởi tạo dữ liệu danh mục, thương hiệu và sản phẩm kính mắt...");

        // 1. Brands
        Brand bolon = brandRepository.save(Brand.builder().name("Bolon").country("France").description("Thương hiệu gọng thời trang cao cấp từ Pháp").build());
        Brand rayban = brandRepository.save(Brand.builder().name("Ray-Ban").country("Italy").description("Biểu tượng kính râm và gọng kính phi công").build());
        Brand charmant = brandRepository.save(Brand.builder().name("Charmant").country("Japan").description("Chuyên gia gọng kính Titanium Nhật Bản").build());
        Brand essilor = brandRepository.save(Brand.builder().name("Essilor").country("France").description("Tập đoàn tròng kính số 1 thế giới").build());
        Brand zeiss = brandRepository.save(Brand.builder().name("Carl Zeiss").country("Germany").description("Quang học đỉnh cao tiêu chuẩn Đức").build());
        Brand chemi = brandRepository.save(Brand.builder().name("Chemi Lens").country("Korea").description("Tròng kính chất lượng cao Hàn Quốc").build());

        // 2. Categories
        Category catFrames = categoryRepository.save(Category.builder().name("Gọng Kính").slug("gong-kinh").description("Gọng kính cận, viễn, loạn thị").build());
        Category catLenses = categoryRepository.save(Category.builder().name("Tròng Kính Khúc Xạ").slug("trong-kinh").description("Tròng đơn tròng, đa tròng kỹ thuật số").build());
        Category catSun = categoryRepository.save(Category.builder().name("Kính Mát").slug("kinh-mat").description("Kính râm chống tia UV & phân cực").build());
        Category catAcc = categoryRepository.save(Category.builder().name("Phụ Kiện").slug("phu-kien").description("Khăn lau, nước rửa, ve mũi, hộp đựng").build());

        // 3. Products & Variants
        // Product 1: Bolon Graceful BJ3120
        Product p1 = productRepository.save(Product.builder()
                .name("Gọng Kính Bolon Graceful BJ3120")
                .slug("gong-kinh-bolon-graceful-bj3120")
                .category(catFrames)
                .brand(bolon)
                .productType(ProductType.FRAME)
                .description("Thiết kế thanh lịch hình đa giác nhẹ nhàng, chất liệu B-Titanium siêu bền siêu nhẹ 7.8g.")
                .status("ACTIVE")
                .build());

        productVariantRepository.save(ProductVariant.builder()
                .product(p1)
                .sku("BL-BJ3120-C10")
                .barcode("8936012340011")
                .name("Bolon BJ3120 - Rose Gold")
                .color("Rose Gold C10")
                .frameShape("Geometrical")
                .material("B-Titanium")
                .price(new BigDecimal("3280000"))
                .costPrice(new BigDecimal("1850000"))
                .stockQuantity(15)
                .imageUrl("https://images.unsplash.com/photo-1591076482161-42ce6da69f67?auto=format&fit=crop&w=600&q=80")
                .build());

        productVariantRepository.save(ProductVariant.builder()
                .product(p1)
                .sku("BL-BJ3120-B30")
                .barcode("8936012340028")
                .name("Bolon BJ3120 - Đen Ánh Bạc")
                .color("Silver Black B30")
                .frameShape("Geometrical")
                .material("B-Titanium")
                .price(new BigDecimal("3280000"))
                .costPrice(new BigDecimal("1850000"))
                .stockQuantity(12)
                .imageUrl("https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=600&q=80")
                .build());

        // Product 2: Ray-Ban Aviator Classic
        Product p2 = productRepository.save(Product.builder()
                .name("Kính Mát Ray-Ban Aviator Classic")
                .slug("kinh-mat-ray-ban-aviator-classic")
                .category(catSun)
                .brand(rayban)
                .productType(ProductType.SUNGLASSES)
                .description("Kính râm phi công trứ danh với tròng thủy tinh G-15 chống lóa hoàn hảo.")
                .status("ACTIVE")
                .build());

        productVariantRepository.save(ProductVariant.builder()
                .product(p2)
                .sku("RB-3025-001")
                .barcode("8936012340035")
                .name("Ray-Ban Aviator Gọng Vàng Tròng Xanh G-15")
                .color("Gold / Green G-15")
                .frameShape("Aviator Pilot")
                .material("Metal Alloy")
                .price(new BigDecimal("4850000"))
                .costPrice(new BigDecimal("2900000"))
                .stockQuantity(8)
                .imageUrl("https://images.unsplash.com/photo-1574258495973-f010dfbb5371?auto=format&fit=crop&w=600&q=80")
                .build());

        // Product 3: Essilor Crizal Sapphire HR
        Product p3 = productRepository.save(Product.builder()
                .name("Tròng Kính Essilor Crizal Sapphire HR 1.67")
                .slug("trong-kinh-essilor-crizal-sapphire-hr-167")
                .category(catLenses)
                .brand(essilor)
                .productType(ProductType.LENS)
                .description("Công nghệ phủ chống phản quang 360 độ siêu việt, độ truyền quang 99.2%, độ bền chống xước gấp 3 lần.")
                .status("ACTIVE")
                .build());

        productVariantRepository.save(ProductVariant.builder()
                .product(p3)
                .sku("LZ-CRIZAL-HR167")
                .barcode("8936012340059")
                .name("Crizal Sapphire HR Chiết Suất 1.67")
                .lensIndex("1.67")
                .material("High-Index MR-7")
                .price(new BigDecimal("2950000"))
                .costPrice(new BigDecimal("1650000"))
                .stockQuantity(40)
                .imageUrl("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?auto=format&fit=crop&w=600&q=80")
                .build());

        // Product 4: Zeiss Single Vision Superb 1.60
        Product p4 = productRepository.save(Product.builder()
                .name("Tròng Kính Carl Zeiss Single Vision Superb 1.60")
                .slug("trong-kinh-zeiss-single-vision-superb-160")
                .category(catLenses)
                .brand(zeiss)
                .productType(ProductType.LENS)
                .description("Tròng phi cầu Freeform chuẩn quang sai độ nét cao, phủ DuraVision Platinum siêu cứng.")
                .status("ACTIVE")
                .build());

        productVariantRepository.save(ProductVariant.builder()
                .product(p4)
                .sku("LZ-ZEISS-SV160")
                .barcode("8936012340073")
                .name("Zeiss Single Vision Superb 1.60")
                .lensIndex("1.60")
                .material("MR-8 Clarity")
                .price(new BigDecimal("3450000"))
                .costPrice(new BigDecimal("2100000"))
                .stockQuantity(30)
                .imageUrl("https://images.unsplash.com/photo-1572635196237-14b3f281503f?auto=format&fit=crop&w=600&q=80")
                .build());

        // Product 5: Khăn lau Nano
        Product p5 = productRepository.save(Product.builder()
                .name("Khăn Lau Nano Chống Bám Hơi Sương 24H")
                .slug("khan-lau-nano-chong-bam-hoi-suong-24h")
                .category(catAcc)
                .brand(essilor)
                .productType(ProductType.ACCESSORY)
                .description("Công nghệ phủ hoạt tính phân tử chống sương mờ khi đeo khẩu trang hoặc uống đồ nóng.")
                .status("ACTIVE")
                .build());

        productVariantRepository.save(ProductVariant.builder()
                .product(p5)
                .sku("ACC-NANO-FOG")
                .barcode("8936012340097")
                .name("Khăn Lau Kính Nano Chống Hơi Nước")
                .material("Microfiber Nano-active")
                .price(new BigDecimal("100000"))
                .costPrice(new BigDecimal("35000"))
                .stockQuantity(120)
                .imageUrl("https://images.unsplash.com/photo-1508296695146-257a814070b4?auto=format&fit=crop&w=600&q=80")
                .build());

        log.info("Đã khởi tạo thành công {} sản phẩm catalog vào db_catalog!", productRepository.count());
    }
}
