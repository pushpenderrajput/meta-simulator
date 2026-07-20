package com.simulator.metawhatsapp.controller;

import com.simulator.metawhatsapp.dto.request.SendMessageRequest;
import com.simulator.metawhatsapp.dto.response.SendMessageResponse;
import com.simulator.metawhatsapp.service.MessageService;
import com.simulator.metawhatsapp.validator.ApiVersionValidator;
import com.simulator.metawhatsapp.validator.MessageContentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MessageController {

    private final ApiVersionValidator apiVersionValidator;
    private final MessageContentValidator messageContentValidator;
    private final MessageService messageService;

    @PostMapping(value = "/{version}/{phoneNumberId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SendMessageResponse> sendMessage(
            @PathVariable String version,
            @PathVariable String phoneNumberId,
            @RequestBody SendMessageRequest request) {

        log.debug("Incoming send-message request: version={} phoneNumberId={} type={}",
                version, phoneNumberId, request.type());

        apiVersionValidator.validate(version);
        messageContentValidator.validate(request);

        SendMessageResponse response = messageService.acceptMessage(phoneNumberId, request);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-App-Usage", "{\"call_count\":0,\"total_cputime\":0,\"total_time\":0}");

        return ResponseEntity.ok().headers(headers).body(response);
    }
}