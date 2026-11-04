package com.glassstore.order.service;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
@Component @ConfigurationProperties(prefix="pos.loyalty") @Data
public class LoyaltyPolicy {
    private boolean enabled = true;
    private BigDecimal pointValue = new BigDecimal("1000");
    private int maxRedemptionPercent = 20;
    private boolean allowVoucherStack = true;
}
