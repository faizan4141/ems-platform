package com.ems.academic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FeeConfig.FeeSettings.class)
public class FeeConfig {

    @ConfigurationProperties(prefix = "ems.fees")
    public record FeeSettings(@DefaultValue("INR") String currency,
                              @DefaultValue("3") int dueSoonDays,
                              @DefaultValue("7") int reminderIntervalDays) {}
}