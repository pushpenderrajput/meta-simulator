package com.simulator.metawhatsapp.dto.request;

import com.simulator.metawhatsapp.dto.request.contacts.ContactCardObject;
import com.simulator.metawhatsapp.dto.request.template.TemplateObject;

import java.util.List;

public record SendMessageRequest(
        String messaging_product,
        String recipient_type,
        String to,
        String type,
        ContextObject context,
        TextObject text,
        MediaObject image,
        MediaObject video,
        MediaObject audio,
        MediaObject document,
        MediaObject sticker,
        LocationObject location,
        ReactionObject reaction,
        List<ContactCardObject> contacts,
        TemplateObject template,
        InteractiveObject interactive
) {
}