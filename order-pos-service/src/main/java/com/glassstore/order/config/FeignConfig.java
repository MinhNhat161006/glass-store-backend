package com.glassstore.order.config;

import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign resilience settings.
 *
 * <p>A catalog or lab call must fail fast rather than hang a checkout request, and retries
 * are disabled on purpose: the operations are already idempotent at the API level, so a
 * blind transport retry could double-charge a stock reservation.</p>
 */
@Configuration
public class FeignConfig {

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }

    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}