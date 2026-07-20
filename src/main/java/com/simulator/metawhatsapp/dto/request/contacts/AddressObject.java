package com.simulator.metawhatsapp.dto.request.contacts;

public record AddressObject(
        String street,
        String city,
        String state,
        String zip,
        String country,
        String country_code,
        String type
) {
}