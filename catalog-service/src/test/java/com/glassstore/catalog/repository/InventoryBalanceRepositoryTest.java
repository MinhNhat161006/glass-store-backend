package com.glassstore.catalog.repository;

import com.glassstore.catalog.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class InventoryBalanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InventoryBalanceRepository balanceRepository;

    @Test
    void decrementIsAtomicAndNeverAllowsNegativeQuantity() {
        Category category = entityManager.persist(Category.builder()
                .name("Frames")
                .slug("frames-test")
                .build());
        Brand brand = entityManager.persist(Brand.builder()
                .name("Test Brand")
                .build());
        Product product = entityManager.persist(Product.builder()
                .name("Test frame")
                .slug("test-frame")
                .category(category)
                .brand(brand)
                .productType(ProductType.FRAME)
                .build());
        ProductVariant variant = entityManager.persist(ProductVariant.builder()
                .product(product)
                .sku("TEST-FRAME-001")
                .barcode("TEST-BARCODE-001")
                .name("Test frame")
                .price(BigDecimal.ONE)
                .costPrice(BigDecimal.ONE)
                .stockQuantity(5)
                .build());
        InventoryLocation location = entityManager.persist(InventoryLocation.builder()
                .code("BRANCH_01")
                .name("Chi nhánh 01")
                .build());
        entityManager.persistAndFlush(InventoryBalance.builder()
                .variant(variant)
                .location(location)
                .quantity(5)
                .build());

        assertEquals(0, balanceRepository.decrementIfAvailable("TEST-FRAME-001", "BRANCH_01", 6));
        entityManager.clear();
        assertEquals(5, balanceRepository.findBySkuAndLocationCode("TEST-FRAME-001", "BRANCH_01")
                .orElseThrow().getQuantity());

        assertEquals(1, balanceRepository.decrementIfAvailable("TEST-FRAME-001", "BRANCH_01", 5));
        entityManager.clear();
        assertEquals(0, balanceRepository.findBySkuAndLocationCode("TEST-FRAME-001", "BRANCH_01")
                .orElseThrow().getQuantity());
    }
}
