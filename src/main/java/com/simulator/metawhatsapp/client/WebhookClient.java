package com.simulator.metawhatsapp.client;

import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.dto.webhook.StatusDetail;
import com.simulator.metawhatsapp.service.StatsService;
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
    private final StatsService statsService;

    public Mono<Void> sendWebhook(String targetUrl, MetaWebhookPayload payload) {
        String status = "unknown";
        String wamid = "UNKNOWN";

        try {
            StatusDetail statusDetail = payload.entry().get(0).changes().get(0).value().statuses().get(0);
            status = statusDetail.status();
            wamid = statusDetail.id();
        } catch (Exception ignored) {}

        final String finalStatus = status;
        final String finalWamid = wamid;

        return webhookWebClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(v -> {
                    statsService.recordDlrDelivered(finalStatus);
                    log.debug("🚀 DLR delivered -> status={} wamid={} targetUrl={}",
                            finalStatus, finalWamid, targetUrl);
                })
                .doOnError(e -> {
                    statsService.incrementDlrFailedToSend();
                    log.warn("⚠️ Receiver error -> status={} wamid={} targetUrl={} reason={}",
                            finalStatus, finalWamid, targetUrl, e.getMessage());
                })
                .then();
    }
}