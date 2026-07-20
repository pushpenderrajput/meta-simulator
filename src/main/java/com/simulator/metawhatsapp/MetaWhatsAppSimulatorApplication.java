package com.simulator.metawhatsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Meta WhatsApp Cloud API Simulator.
 *
 * <p>This application is intentionally stateless: no database, no message broker,
 * no caching layer. Every request is handled, any webhook callbacks required are
 * scheduled asynchronously in-memory (via {@code @Async} + a dedicated
 * {@link java.util.concurrent.Executor}), and once those callbacks are dispatched
 * the simulator retains nothing about the request. The CPaaS platform under test
 * is solely responsible for persistence.</p>
 */
@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan("com.simulator.metawhatsapp.properties")
public class MetaWhatsAppSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetaWhatsAppSimulatorApplication.class, args);
    }
}
