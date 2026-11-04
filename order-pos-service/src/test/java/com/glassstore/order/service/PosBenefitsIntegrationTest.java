package com.glassstore.order.service;
import com.glassstore.order.entity.*;
import com.glassstore.order.repository.*;
import com.glassstore.order.client.CatalogInventoryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:benefits;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect","inventory.outbox.poll-interval-ms=3600000"})
@Transactional
class PosBenefitsIntegrationTest {
    @Autowired OrderService orders;
    @Autowired PromotionService promotions;
    @Autowired OrderCancellationService cancellations;
    @Autowired VoucherRepository vouchers;
    @Autowired LoyaltyAccountRepository accounts;
    @Autowired LoyaltyEntryRepository entries;
    @Autowired PaymentRepository payments;
    @Autowired OrderRepository orderRepository;
    @MockBean PosDataService data;
    @MockBean CatalogInventoryClient catalog;
    @MockBean StockOutboxWorker worker;
    @BeforeEach void setup() throws Exception {
        when(data.actor(any(),anyBoolean())).thenReturn(new ObjectMapper().readTree("{\"id\":1,\"username\":\"qa\",\"roles\":[\"ROLE_ADMIN\"]}"));
        when(data.catalogPrices()).thenReturn(Map.of("TEST-SKU",new BigDecimal("1000000")));
        Voucher v=new Voucher();v.setCode("QA10");v.setType("PERCENT");v.setAmount(new BigDecimal("10"));v.setMinimumAmount(new BigDecimal("500000"));v.setMaximumDiscount(new BigDecimal("100000"));v.setUsageLimit(1);v.setExpiresAt(LocalDateTime.now().plusDays(1));vouchers.save(v);
        LoyaltyAccount account=new LoyaltyAccount();account.setCustomerId(10L);account.setBalance(100);accounts.save(account);
    }
    private Order order() {
        return Order.builder().channel("POS").branchCode("BRANCH_01").customerId(10L).customerName("QA").customerPhone("").paymentMethod("CASH")
            .items(new ArrayList<>(List.of(OrderItem.builder().skuId("TEST-SKU").productName("Test glasses").quantity(1).unitPrice(BigDecimal.ONE).build()))).build();
    }
    @Test void quoteDoesNotSpendAndCheckoutAndCancellationAreIdempotent() {
        Order request=order();request.setVoucherCode("QA10");request.setPointsToUse(50L);request.setCashReceived(new BigDecimal("2000000"));
        PromotionService.Quote quote=promotions.calculate(request,"Bearer test",false);
        assertEquals(0,quote.finalAmount().compareTo(new BigDecimal("918000")));
        assertEquals(100,accounts.findById(10L).orElseThrow().getBalance());assertEquals(0,vouchers.findByCode("QA10").orElseThrow().getUsedCount());
        Order saved=orders.create(request,"QA-payment-1","Bearer test").getBody();
        assertEquals(50,accounts.findById(10L).orElseThrow().getBalance());assertEquals(1,vouchers.findByCode("QA10").orElseThrow().getUsedCount());
        assertEquals(saved.getId(),orders.create(order(),"QA-payment-1","Bearer test").getBody().getId());
        assertEquals(1,payments.findByOrderId(saved.getId()).size());assertEquals(50,accounts.findById(10L).orElseThrow().getBalance());
        cancellations.cancel(saved.getId(),new OrderCancellationService.Request("Khách đổi ý",false),"Bearer test");
        cancellations.complete(saved.getId());cancellations.complete(saved.getId());
        cancellations.cancel(saved.getId(),new OrderCancellationService.Request("Lặp yêu cầu",false),"Bearer test");
        assertEquals(100,accounts.findById(10L).orElseThrow().getBalance());assertEquals(0,vouchers.findByCode("QA10").orElseThrow().getUsedCount());
        assertEquals(2,entries.findByCustomerIdOrderByCreatedAtDesc(10L).size());
        assertEquals("REFUND_PENDING",payments.findByOrderId(saved.getId()).get(0).getStatus());
        cancellations.confirmRefund(saved.getId(),"Bearer test");assertEquals("REFUNDED",payments.findByOrderId(saved.getId()).get(0).getStatus());
    }
    @Test void rejectsGuestPointsExpiredVoucherAndInsufficientCashWithoutSpending() {
        Order guest=order();guest.setCustomerId(null);guest.setPointsToUse(1L);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->promotions.calculate(guest,"Bearer test",false));
        Voucher expired=vouchers.findByCode("QA10").orElseThrow();expired.setExpiresAt(LocalDateTime.now().minusDays(1));vouchers.save(expired);
        Order request=order();request.setVoucherCode("QA10");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->promotions.calculate(request,"Bearer test",false));
        Order poor=order();poor.setCashReceived(BigDecimal.ONE);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->orders.create(poor,"QA-poor","Bearer test"));
        assertEquals(100,accounts.findById(10L).orElseThrow().getBalance());assertEquals(0,orderRepository.count());
    }
}
