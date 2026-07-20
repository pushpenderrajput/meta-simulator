package com.simulator.metawhatsapp.dto.request.interactive;

import java.util.List;
import java.util.Map;

public record ActionObject(
        String button,
        List<ButtonObject> buttons,
        List<SectionObject> sections,
        String catalog_id,
        String product_retailer_id,
        String name,
        Map<String, Object> parameters
) {
}