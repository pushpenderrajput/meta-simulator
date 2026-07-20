package com.simulator.metawhatsapp.client;
import reactor.util.retry.Retry;
import java.time.Duration;
import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookClient {

    private final WebClient webhookWebClient;
    private final SimulatorProperties properties;

    /**
     * Dispatches a wire-compatible webhook payload asynchronously to the target CPaaS application.
     * Uses a non-blocking reactive stream that handles responses out-of-band.
     *
     * @param payload The structured Meta webhook containing the message statuses.
     */
    public void sendWebhook(MetaWebhookPayload payload) {
        String targetUrl = properties.webhook().callbackUrl();

        log.debug("Dispatching outbound DLR payload to target URL: {}", targetUrl);

        webhookWebClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(Retry.backoff(
                        properties.webhook().retry().maxAttempts(),
                        Duration.ofSeconds(1)
                ))
                .doOnSuccess(response -> log.info("Successfully delivered webhook DLR to target application. Status={}", response.getStatusCode()))
                .doOnError(error -> log.error("Failed to deliver webhook DLR to target application. Error={}", error.getMessage()))
                .subscribe();
    }
}