package com.glassstore.catalog.service;

import com.glassstore.catalog.dto.CreateStockTransferRequest;
import com.glassstore.catalog.dto.StockTransferDTO;
import com.glassstore.catalog.entity.*;
import com.glassstore.catalog.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stock-transfer-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class StockTransferIntegrationTest {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryLocationRepository locationRepository;
    @Autowired
    private InventoryBalanceRepository balanceRepository;
    @Autowired
    private InventoryMovementRepository movementRepository;
    @Autowired
    private ProductVariantRepository variantRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private StockTransferRepository transferRepository;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsPendingTransferWithoutChangingStockAndReceivesBothDirectionsOnce() {
        TestStock stock = createStock(20, 5);
        StockTransferDTO pending = inventoryService.createTransfer(
                request(stock.sku(), InventoryService.LAB, InventoryService.BRANCH_01, 3), "lab.operator");

        assertEquals("PENDING", pending.getStatus());
        assertEquals("lab.operator", pending.getCreatedBy());
        assertBalances(stock.sku(), 20, 5);
        assertEquals(0, movementsFor(pending.getTransferCode()).size());

        StockTransferDTO received = inventoryService.receiveTransfer(pending.getId(), "admin.operator");
        assertEquals("RECEIVED", received.getStatus());
        assertEquals("admin.operator", received.getReceivedBy());
        assertBalances(stock.sku(), 17, 8);
        assertEquals(25, variantRepository.findBySku(stock.sku()).orElseThrow().getStockQuantity());
        assertEquals(2, movementsFor(pending.getTransferCode()).size());
        assertThrows(ResponseStatusException.class,
                () -> inventoryService.receiveTransfer(pending.getId(), "admin.operator"));
        assertBalances(stock.sku(), 17, 8);
        assertEquals(2, movementsFor(pending.getTransferCode()).size());

        StockTransferDTO reversePending = inventoryService.createTransfer(
                request(stock.sku(), InventoryService.BRANCH_01, InventoryService.LAB, 2), "admin.operator");
        assertBalances(stock.sku(), 17, 8);
        inventoryService.receiveTransfer(reversePending.getId(), "lab.operator");
        assertBalances(stock.sku(), 19, 6);
        assertEquals(25, variantRepository.findBySku(stock.sku()).orElseThrow().getStockQuantity());
    }

    @Test
    void rejectsInvalidDraftsAndInsufficientSourceStock() {
        TestStock stock = createStock(4, 2);

        assertStatus(HttpStatus.BAD_REQUEST, () -> inventoryService.createTransfer(
                request(stock.sku(), InventoryService.LAB, InventoryService.LAB, 1), "admin"));
        assertStatus(HttpStatus.BAD_REQUEST, () -> inventoryService.createTransfer(
                request(stock.sku(), InventoryService.LAB, InventoryService.BRANCH_01, 0), "admin"));
        assertStatus(HttpStatus.CONFLICT, () -> inventoryService.createTransfer(
                request(stock.sku(), InventoryService.LAB, InventoryService.BRANCH_01, 5), "admin"));
        assertStatus(HttpStatus.NOT_FOUND, () -> inventoryService.createTransfer(
                request("UNKNOWN-" + stock.sku(), InventoryService.LAB, InventoryService.BRANCH_01, 1), "admin"));
        CreateStockTransferRequest duplicate = request(
                stock.sku(), InventoryService.LAB, InventoryService.BRANCH_01, 1);
        duplicate.setItems(List.of(item(stock.sku(), 1), item(stock.sku(), 1)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> inventoryService.createTransfer(duplicate, "admin"));
        CreateStockTransferRequest empty = request(stock.sku(), InventoryService.LAB, InventoryService.BRANCH_01, 1);
        empty.setItems(List.of());
        assertStatus(HttpStatus.BAD_REQUEST, () -> inventoryService.createTransfer(empty, "admin"));
        assertBalances(stock.sku(), 4, 2);
    }

    @Test
    void insufficientStockAtReceiptRollsBackEverySkuAndKeepsTransferPending() {
        TestStock first = createStock(10, 0);
        TestStock second = createStock(10, 0);
        CreateStockTransferRequest request = request(first.sku(), InventoryService.LAB, InventoryService.BRANCH_01, 2);
        request.setItems(List.of(item(first.sku(), 2), item(second.sku(), 2)));
        StockTransferDTO transfer = inventoryService.createTransfer(request, "lab.operator");

        InventoryBalance secondSource = balanceRepository.findBySkuAndLocationCode(
                second.sku(), InventoryService.LAB).orElseThrow();
        secondSource.setQuantity(1);
        balanceRepository.save(secondSource);

        assertStatus(HttpStatus.CONFLICT,
                () -> inventoryService.receiveTransfer(transfer.getId(), "admin.operator"));
        assertBalances(first.sku(), 10, 0);
        assertBalances(second.sku(), 1, 0);
        assertTrue(movementsFor(transfer.getTransferCode()).isEmpty());
        assertEquals("PENDING", transferRepository.findById(transfer.getId()).orElseThrow().getStatus().name());
    }

    @Test
    void concurrentReceiptRequestsUpdateStockOnlyOnce() throws Exception {
        TestStock stock = createStock(10, 0);
        StockTransferDTO transfer = inventoryService.createTransfer(
                request(stock.sku(), InventoryService.LAB, InventoryService.BRANCH_01, 4), "lab.operator");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> receiveAfter(start, transfer.getId()));
            Future<Boolean> second = executor.submit(() -> receiveAfter(start, transfer.getId()));
            start.countDown();
            assertEquals(1, (int) List.of(first.get(), second.get()).stream().filter(Boolean::booleanValue).count());
        } finally {
            executor.shutdownNow();
        }

        assertBalances(stock.sku(), 6, 4);
        assertEquals(2, movementsFor(transfer.getTransferCode()).size());
    }

    @Test
    void legacyInTransitReceiptCreditsDestinationWithoutDeductingSourceTwice() {
        TestStock stock = createStock(10, 2);
        StockTransferDTO draft = inventoryService.createTransfer(
                request(stock.sku(), InventoryService.LAB, InventoryService.BRANCH_01, 3), "lab.operator");
        StockTransfer legacyTransfer = transferRepository.findById(draft.getId()).orElseThrow();
        legacyTransfer.setStatus(StockTransferStatus.IN_TRANSIT);
        legacyTransfer.setDispatchedAt(LocalDateTime.now());
        transferRepository.save(legacyTransfer);

        balanceRepository.findBySkuAndLocationCode(stock.sku(), InventoryService.LAB)
                .ifPresent(balance -> {
                    balance.setQuantity(7);
                    balanceRepository.save(balance);
                });
        ProductVariant variant = variantRepository.findBySku(stock.sku()).orElseThrow();
        variant.setStockQuantity(9);
        variantRepository.save(variant);

        inventoryService.receiveTransfer(draft.getId(), "lab.operator");
        assertBalances(stock.sku(), 7, 5);
        assertEquals(12, variantRepository.findBySku(stock.sku()).orElseThrow().getStockQuantity());
    }

    @Test
    void inventoryEndpointsRequireExistingPortalRoles() throws Exception {
        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/inventory")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_POS"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/inventory")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LAB"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/inventory")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LAB_TECH"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/inventory")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SALES_STAFF"))))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/inventory/transfers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SALES_STAFF"))))
                .andExpect(status().isForbidden());
    }

    private boolean receiveAfter(CountDownLatch start, Long transferId) throws Exception {
        start.await();
        try {
            inventoryService.receiveTransfer(transferId, "admin.operator");
            return true;
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            return false;
        }
    }

    private TestStock createStock(int labQuantity, int branchQuantity) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Brand brand = brandRepository.save(Brand.builder().name("Transfer-" + suffix).build());
        Category category = categoryRepository.save(Category.builder()
                .name("Transfer-" + suffix)
                .slug("transfer-" + suffix)
                .build());
        Product product = productRepository.save(Product.builder()
                .name("Transfer " + suffix)
                .slug("transfer-product-" + suffix)
                .category(category)
                .brand(brand)
                .productType(ProductType.FRAME)
                .build());
        String sku = "TR-" + suffix.substring(0, 20);
        ProductVariant variant = variantRepository.save(ProductVariant.builder()
                .product(product)
                .sku(sku)
                .barcode("BAR-" + suffix)
                .name("Transfer item")
                .price(BigDecimal.ONE)
                .costPrice(BigDecimal.ONE)
                .stockQuantity(labQuantity + branchQuantity)
                .build());
        InventoryLocation lab = locationRepository.findByCodeAndActiveTrue(InventoryService.LAB).orElseThrow();
        InventoryLocation branch = locationRepository.findByCodeAndActiveTrue(InventoryService.BRANCH_01).orElseThrow();
        balanceRepository.saveAll(List.of(
                InventoryBalance.builder().variant(variant).location(lab).quantity(labQuantity).build(),
                InventoryBalance.builder().variant(variant).location(branch).quantity(branchQuantity).build()));
        return new TestStock(sku);
    }

    private CreateStockTransferRequest request(
            String sku, String sourceCode, String destinationCode, int quantity) {
        CreateStockTransferRequest request = new CreateStockTransferRequest();
        request.setSourceLocationCode(sourceCode);
        request.setDestinationLocationCode(destinationCode);
        request.setReason("integration test");
        request.setItems(List.of(item(sku, quantity)));
        return request;
    }

    private CreateStockTransferRequest.Item item(String sku, int quantity) {
        CreateStockTransferRequest.Item item = new CreateStockTransferRequest.Item();
        item.setSku(sku);
        item.setQuantity(quantity);
        return item;
    }

    private void assertBalances(String sku, int expectedLab, int expectedBranch) {
        assertEquals(expectedLab, balanceRepository.findBySkuAndLocationCode(sku, InventoryService.LAB)
                .orElseThrow().getQuantity());
        assertEquals(expectedBranch, balanceRepository.findBySkuAndLocationCode(sku, InventoryService.BRANCH_01)
                .orElseThrow().getQuantity());
    }

    private List<InventoryMovement> movementsFor(String transferCode) {
        return movementRepository.findByFilters(null, null, transferCode,
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
    }

    private void assertStatus(HttpStatus expected, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expected, exception.getStatusCode());
    }

    private record TestStock(String sku) {
    }
}
