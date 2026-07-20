package com.simulator.metawhatsapp.config;

import com.simulator.metawhatsapp.properties.SimulatorProperties;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webhookWebClient(SimulatorProperties properties) {
        int timeoutSeconds = properties.webhook().timeoutSeconds();

        ConnectionProvider provider = ConnectionProvider.builder("custom-dlr-pool")
                .maxConnections(2000)
                .pendingAcquireMaxCount(10000)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofSeconds(timeoutSeconds).toMillis())
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}