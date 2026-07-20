package com.simulator.metawhatsapp.dto.request.template;

public record CurrencyObject(
        String fallback_value,
        String code,
        Long amount_1000
) {
}