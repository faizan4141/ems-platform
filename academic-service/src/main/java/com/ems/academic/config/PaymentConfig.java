package com.ems.academic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentConfig.PaymentSettings.class)
public class PaymentConfig {

    @ConfigurationProperties(prefix = "ems.payments")
    public record PaymentSettings(@DefaultValue("simulated") String provider,
                                  @DefaultValue("dev-only-secret-change-me") String webhookSecret,
                                  @DefaultValue("http://localhost:5173") String appBaseUrl,
                                  @DefaultValue("30") int pendingWindowMinutes) {}
}