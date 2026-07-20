package com.simulator.metawhatsapp.dto.webhook;

public record ChangeWrapper(
        ChangeValue value,
        String field
) {
    public static ChangeWrapper of(ChangeValue value) {
        return new ChangeWrapper(value, "messages");
    }
}