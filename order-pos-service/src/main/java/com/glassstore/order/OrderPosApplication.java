package com.glassstore.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class OrderPosApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderPosApplication.class, args);
    }
}
