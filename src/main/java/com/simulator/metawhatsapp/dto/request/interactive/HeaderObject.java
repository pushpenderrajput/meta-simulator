package com.simulator.metawhatsapp.dto.request.interactive;

import com.simulator.metawhatsapp.dto.request.MediaObject;

public record HeaderObject(
        String type,
        String text,
        MediaObject video,
        MediaObject image,
        MediaObject document
) {
}