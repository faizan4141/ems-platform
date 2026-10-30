package com.ems.user.config;

import com.ems.user.event.UserEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
class KafkaTopicConfig {

    @Bean
    NewTopic userEventsTopic() {
        return TopicBuilder.name(UserEventPublisher.TOPIC).partitions(3).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(3).toMillis()))
                .build();
    }
}