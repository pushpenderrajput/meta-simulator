package com.simulator.metawhatsapp.dto.request.template;

import com.simulator.metawhatsapp.dto.request.MediaObject;

public record ParameterObject(
        String type,
        String text,
        CurrencyObject currency,
        DateTimeObject date_time,
        MediaObject image,
        MediaObject document,
        MediaObject video,
        String payload
) {
}