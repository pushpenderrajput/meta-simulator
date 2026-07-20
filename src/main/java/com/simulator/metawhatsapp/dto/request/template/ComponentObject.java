package com.simulator.metawhatsapp.dto.request.template;

import java.util.List;

public record ComponentObject(
        String type,
        String sub_type,
        String index,
        List<ParameterObject> parameters
) {
}