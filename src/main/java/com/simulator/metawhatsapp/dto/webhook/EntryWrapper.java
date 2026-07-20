package com.simulator.metawhatsapp.dto.webhook;

import java.util.List;

public record EntryWrapper(
        String id,
        List<ChangeWrapper> changes
) {}