package com.simulator.metawhatsapp.dto.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.simulator.metawhatsapp.dto.response.ErrorDetail;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StatusDetail(
        String id,
        String status,
        String timestamp,
        String recipient_id,
        ErrorDetail errors // Omitted unless terminal outcome is 'failed' (Phase 4)
) {
}