package com.simulator.metawhatsapp.dto.request;

public record MediaObject(
        String id,
        String link,
        String caption,
        String filename
) {
    public boolean hasExactlyOneSource() {
        boolean hasId = id != null && !id.isBlank();
        boolean hasLink = link != null && !link.isBlank();
        return hasId ^ hasLink;
    }
}