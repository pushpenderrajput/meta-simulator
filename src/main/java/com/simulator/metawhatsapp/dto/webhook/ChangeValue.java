package com.simulator.metawhatsapp.dto.webhook;

import java.util.List;

public record ChangeValue(
        String messaging_product,
        Metadata metadata,
        List<StatusDetail> statuses
) {
}