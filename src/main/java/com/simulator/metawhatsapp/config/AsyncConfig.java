package com.simulator.metawhatsapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * Dedicated thread pools for the simulator's asynchronous work.
 *
 * <p>The HTTP-handling thread must never block waiting on webhook delivery.
 * {@link #webhookTaskExecutor()} backs all {@code @Async} DLR-generation work,
 * and {@link #webhookTaskScheduler()} backs the delayed (accepted -> sent ->
 * delivered -> read) scheduling used instead of {@code Thread.sleep()}.</p>
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    public static final String WEBHOOK_EXECUTOR = "webhookTaskExecutor";
    public static final String WEBHOOK_SCHEDULER = "webhookTaskScheduler";

    @Override
    @Bean(WEBHOOK_EXECUTOR)
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("webhook-async-");
        executor.setTaskDecorator(mdcPropagatingDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * Scheduler used to fire delayed DLR stages (sent/delivered/read) without
     * ever blocking a request-handling thread on {@code Thread.sleep()}.
     */
    @Bean(WEBHOOK_SCHEDULER)
    public ThreadPoolTaskScheduler webhookTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(64); // Increased for high-throughput DLR scheduling
        scheduler.setThreadNamePrefix("webhook-sched-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Keeps correlation/log context consistent across thread hand-off so log
     * lines for a single simulated message can still be traced end-to-end.
     */
    private TaskDecorator mdcPropagatingDecorator() {
        return runnable -> {
            var contextMap = org.slf4j.MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        org.slf4j.MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    org.slf4j.MDC.clear();
                }
            };
        };
    }
}
