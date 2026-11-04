package com.glassstore.catalog.controller;

import com.glassstore.catalog.service.ProductService;
import com.glassstore.catalog.entity.ProductType;
import com.glassstore.catalog.dto.ProductResponseDTO;
import com.glassstore.catalog.dto.ProductVariantDTO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LensRecommendationTest {
    @Test
    void recommendsAtBoundariesAndUsesStrongerEye() {
        ProductService service = mock(ProductService.class);
        when(service.getAllProducts(null, ProductType.LENS, "ACTIVE")).thenReturn(List.of());
        ProductController controller = new ProductController(service);
        assertEquals(List.of("1.56", "1.60"), result(controller, "-2.5", "0").indexes());
        assertEquals(List.of("1.67"), result(controller, "0", "-2.75").indexes());
        assertEquals(List.of("1.67"), result(controller, "-5", "0").indexes());
        assertEquals(List.of("1.74"), result(controller, "0", "-5.25").indexes());
        assertTrue(result(controller, "6", "0").highPower());
    }

    @Test
    void onlyReturnsActiveMatchingFamilyAndIndex() {
        ProductService service = mock(ProductService.class);
        ProductVariantDTO rock = ProductVariantDTO.builder().id(1L).sku("ROCK-167")
                .name("Rock").lensIndex("1.67").status("ACTIVE").build();
        ProductVariantDTO other = ProductVariantDTO.builder().id(2L).sku("OTHER-167")
                .name("Other").lensIndex("1.67").status("ACTIVE").build();
        when(service.getAllProducts(null, ProductType.LENS, "ACTIVE")).thenReturn(List.of(
                ProductResponseDTO.builder().name("Lens").variants(List.of(rock, other)).build()));
        assertEquals(List.of(rock), result(new ProductController(service), "-3", "0").variants());
    }

    private ProductController.LensRecommendation result(ProductController controller, String od, String os) {
        return controller.recommendLenses(new BigDecimal(od), new BigDecimal(os)).getBody().getData();
    }
}
