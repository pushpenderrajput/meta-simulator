package com.simulator.metawhatsapp.dto.request.template;

import java.util.List;

public record TemplateObject(
        String name,
        LanguageObject language,
        List<ComponentObject> components
) {
}