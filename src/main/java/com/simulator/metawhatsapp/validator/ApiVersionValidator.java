package com.simulator.metawhatsapp.validator;

import com.simulator.metawhatsapp.exception.MetaApiException;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiVersionValidator {

    private final SimulatorProperties properties;

    public ApiVersionValidator(SimulatorProperties properties) {
        this.properties = properties;
    }

    public void validate(String version) {
        if (version == null || !properties.apiVersion().supported().contains(version)) {
            throw new MetaApiException(
                    HttpStatus.BAD_REQUEST,
                    "GraphMethodException",
                    100,
                    "Unsupported Graph API version '%s'. Supported versions: %s"
                            .formatted(version, properties.apiVersion().supported())
            );
        }
    }
}