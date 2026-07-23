package com.pthanureddy.markets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MarketRiskApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketRiskApplication.class, args);
    }
}

