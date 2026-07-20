package com.simulator.metawhatsapp.dto.request.contacts;

public record NameObject(
        String formatted_name,
        String first_name,
        String last_name,
        String middle_name,
        String suffix,
        String prefix
) {
}