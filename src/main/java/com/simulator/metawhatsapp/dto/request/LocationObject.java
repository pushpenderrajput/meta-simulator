package com.simulator.metawhatsapp.dto.request;

public record LocationObject(
        Double longitude,
        Double latitude,
        String name,
        String address
) {
}