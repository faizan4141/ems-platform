package com.ems.academic.config;

import com.ems.academic.event.AttendanceEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import com.ems.academic.event.FeeEventPublisher;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "ems.kafka.publish", havingValue = "true", matchIfMissing = true)
class KafkaTopicConfig {

    @Bean
    NewTopic attendanceEventsTopic() {
        return TopicBuilder.name(AttendanceEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                // the events carry guardian contact details, so keep them briefly
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(3).toMillis()))
                .build();
    }

    @Bean
    NewTopic feeEventsTopic() {
        return TopicBuilder.name(FeeEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(3).toMillis()))
                .build();
    }
}