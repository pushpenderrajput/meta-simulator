package com.simulator.metawhatsapp.validator;

import com.simulator.metawhatsapp.dto.request.InteractiveObject;
import com.simulator.metawhatsapp.dto.request.LocationObject;
import com.simulator.metawhatsapp.dto.request.MediaObject;
import com.simulator.metawhatsapp.dto.request.ReactionObject;
import com.simulator.metawhatsapp.dto.request.SendMessageRequest;
import com.simulator.metawhatsapp.dto.request.SupportedMessageType;
import com.simulator.metawhatsapp.dto.request.TextObject;
import com.simulator.metawhatsapp.dto.request.contacts.ContactCardObject;
import com.simulator.metawhatsapp.dto.request.interactive.ActionObject;
import com.simulator.metawhatsapp.dto.request.template.TemplateObject;
import com.simulator.metawhatsapp.exception.MetaApiException;
import com.simulator.metawhatsapp.util.PhoneNumberUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class MessageContentValidator {

    private static final int MAX_TEXT_BODY_LENGTH = 4096;
    private static final Set<String> VALID_RECIPIENT_TYPES = Set.of("individual", "group");
    private static final Set<String> VALID_INTERACTIVE_TYPES = Set.of(
            "button", "list", "product", "product_list", "cta_url", "flow",
            "location_request_message", "catalog_message"
    );

    public void validate(SendMessageRequest request) {
        validateEnvelope(request);
        SupportedMessageType type = resolveType(request.type());

        switch (type) {
            case text -> validateText(request.text());
            case image -> validateMedia(request.image(), "image", true);
            case video -> validateMedia(request.video(), "video", true);
            case audio -> validateMedia(request.audio(), "audio", false);
            case document -> validateMedia(request.document(), "document", true);
            case sticker -> validateMedia(request.sticker(), "sticker", false);
            case location -> validateLocation(request.location());
            case reaction -> validateReaction(request.reaction());
            case contacts -> validateContacts(request.contacts());
            case template -> validateTemplate(request.template());
            case interactive -> validateInteractive(request.interactive());
        }
    }

    private void validateEnvelope(SendMessageRequest request) {
        if (isBlank(request.messaging_product()) || !"whatsapp".equals(request.messaging_product())) {
            throw invalidParameter("messaging_product must be \"whatsapp\"");
        }
        if (request.recipient_type() != null && !VALID_RECIPIENT_TYPES.contains(request.recipient_type())) {
            throw invalidParameter("recipient_type must be one of " + VALID_RECIPIENT_TYPES);
        }
        if (isBlank(request.to())) {
            throw invalidParameter("'to' is a required field");
        }
        if (!PhoneNumberUtil.isPlausiblePhoneNumber(request.to())) {
            throw invalidParameter("'to' is not a valid phone number");
        }
        if (isBlank(request.type())) {
            throw invalidParameter("'type' is a required field");
        }
    }

    private SupportedMessageType resolveType(String type) {
        try {
            return SupportedMessageType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            throw new MetaApiException(
                    HttpStatus.BAD_REQUEST,
                    "OAuthException",
                    131051,
                    "Message type \"%s\" is not supported".formatted(type)
            );
        }
    }

    private void validateText(TextObject text) {
        if (text == null || isBlank(text.body())) {
            throw invalidParameter("'text.body' is a required field for type=text");
        }
        if (text.body().length() > MAX_TEXT_BODY_LENGTH) {
            throw invalidParameter("'text.body' must not exceed " + MAX_TEXT_BODY_LENGTH + " characters");
        }
    }

    private void validateMedia(MediaObject media, String fieldName, boolean captionAllowed) {
        if (media == null) {
            throw invalidParameter("'" + fieldName + "' object is required for type=" + fieldName);
        }
        if (!media.hasExactlyOneSource()) {
            throw invalidParameter("'" + fieldName + "' requires exactly one of 'id' or 'link'");
        }
        if (!captionAllowed && media.caption() != null) {
            throw invalidParameter("'caption' is not supported for type=" + fieldName);
        }
    }

    private void validateLocation(LocationObject location) {
        if (location == null || location.latitude() == null || location.longitude() == null) {
            throw invalidParameter("'location.latitude' and 'location.longitude' are required for type=location");
        }
        if (location.latitude() < -90 || location.latitude() > 90) {
            throw invalidParameter("'location.latitude' must be between -90 and 90");
        }
        if (location.longitude() < -180 || location.longitude() > 180) {
            throw invalidParameter("'location.longitude' must be between -180 and 180");
        }
    }

    private void validateReaction(ReactionObject reaction) {
        if (reaction == null || isBlank(reaction.message_id())) {
            throw invalidParameter("'reaction.message_id' is a required field for type=reaction");
        }
        if (reaction.emoji() == null) {
            throw invalidParameter("'reaction.emoji' is a required field for type=reaction (use an empty string to remove a reaction)");
        }
    }

    private void validateContacts(List<ContactCardObject> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            throw invalidParameter("'contacts' must contain at least one contact for type=contacts");
        }
        for (ContactCardObject contact : contacts) {
            if (contact.name() == null || isBlank(contact.name().formatted_name())) {
                throw invalidParameter("'contacts[].name.formatted_name' is a required field");
            }
        }
    }

    private void validateTemplate(TemplateObject template) {
        if (template == null || isBlank(template.name())) {
            throw invalidParameter("'template.name' is a required field for type=template");
        }
        if (template.language() == null || isBlank(template.language().code())) {
            throw invalidParameter("'template.language.code' is a required field for type=template");
        }
    }

    private void validateInteractive(InteractiveObject interactive) {
        if (interactive == null || isBlank(interactive.type())) {
            throw invalidParameter("'interactive.type' is a required field for type=interactive");
        }
        if (!VALID_INTERACTIVE_TYPES.contains(interactive.type())) {
            throw invalidParameter("'interactive.type' must be one of " + VALID_INTERACTIVE_TYPES);
        }
        if (interactive.action() == null) {
            throw invalidParameter("'interactive.action' is a required field for type=interactive");
        }

        ActionObject action = interactive.action();
        switch (interactive.type()) {
            case "button" -> {
                if (action.buttons() == null || action.buttons().isEmpty()) {
                    throw invalidParameter("'interactive.action.buttons' must contain at least one button");
                }
                if (action.buttons().size() > 3) {
                    throw invalidParameter("'interactive.action.buttons' supports a maximum of 3 buttons");
                }
            }
            case "list" -> {
                if (isBlank(action.button())) {
                    throw invalidParameter("'interactive.action.button' (the list's label) is required for type=list");
                }
                if (action.sections() == null || action.sections().isEmpty()) {
                    throw invalidParameter("'interactive.action.sections' must contain at least one section for type=list");
                }
            }
            default -> {
                // product/product_list/cta_url/flow/location_request_message/catalog_message:
                // action payloads vary too widely to usefully hard-validate further here.
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private MetaApiException invalidParameter(String detail) {
        return new MetaApiException(HttpStatus.BAD_REQUEST, "OAuthException", 100, "(#100) " + detail);
    }
}