package com.ems.tenant.config;

import com.ems.tenant.event.TenantEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic tenantEventsTopic() {
        return TopicBuilder.name(TenantEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)   // 1 locally; use 3 in production
                .build();
    }
}