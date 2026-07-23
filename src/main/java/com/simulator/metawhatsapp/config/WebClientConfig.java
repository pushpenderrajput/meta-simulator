package com.simulator.metawhatsapp.config;

import com.simulator.metawhatsapp.properties.SimulatorProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webhookWebClient(SimulatorProperties properties) {
        int timeoutSeconds = properties.webhook().timeoutSeconds();

        // Optimized Connection Pool for 1,500+ Outbound DLR TPS
        ConnectionProvider provider = ConnectionProvider.builder("custom-dlr-pool")
                .maxConnections(3000)                          // Expanded pool capacity for high TPS
                .pendingAcquireMaxCount(20000)                 // Queue size for acquiring connections
                .pendingAcquireTimeout(Duration.ofSeconds(5))  // Wait time before throwing connection pool exception
                .maxIdleTime(Duration.ofSeconds(20))           // Close idle connections after 20s
                .maxLifeTime(Duration.ofSeconds(60))           // Recycle connections every 60s
                .evictInBackground(Duration.ofSeconds(10))    // Periodically clean evicted connections
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofSeconds(timeoutSeconds).toMillis())
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}