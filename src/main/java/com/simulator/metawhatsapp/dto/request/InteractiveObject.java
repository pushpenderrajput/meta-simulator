package com.simulator.metawhatsapp.dto.request;

import com.simulator.metawhatsapp.dto.request.interactive.ActionObject;
import com.simulator.metawhatsapp.dto.request.interactive.BodyObject;
import com.simulator.metawhatsapp.dto.request.interactive.FooterObject;
import com.simulator.metawhatsapp.dto.request.interactive.HeaderObject;

public record InteractiveObject(
        String type,
        HeaderObject header,
        BodyObject body,
        FooterObject footer,
        ActionObject action
) {
}