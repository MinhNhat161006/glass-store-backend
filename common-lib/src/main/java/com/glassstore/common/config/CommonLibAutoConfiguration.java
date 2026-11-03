package com.glassstore.common.config;

import com.glassstore.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers the shared error handling for any service that puts {@code common-lib}
 * on its classpath, including through a plain (non-Boot) component scan.
 */
@AutoConfiguration
public class CommonLibAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}