package com.karasuno.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's @Async support for non-blocking audit log writes.
 * Audit events are recorded asynchronously so they never add latency to request threads.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Uses Spring Boot's default SimpleAsyncTaskExecutor.
    // For high-throughput production, replace with a configured ThreadPoolTaskExecutor.
}
