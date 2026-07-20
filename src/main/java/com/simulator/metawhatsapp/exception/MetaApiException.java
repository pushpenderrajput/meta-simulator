package com.simulator.metawhatsapp.exception;

import com.simulator.metawhatsapp.dto.response.ErrorData;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Carries everything required to render a byte-for-byte Meta-compatible
 * error response: the HTTP status Meta would use, and the {@code type},
 * {@code code}, {@code error_subcode}, and {@code error_data} fields of
 * the error body.
 *
 * <p>Thrown by validators/services and translated into a
 * {@link com.simulator.metawhatsapp.dto.response.MetaErrorResponse} by the
 * global exception handler (see Phase 4).</p>
 */
@Getter
public class MetaApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String type;
    private final int code;
    private final Integer errorSubcode;
    private final ErrorData errorData;

    public MetaApiException(HttpStatus httpStatus,
                             String type,
                             int code,
                             Integer errorSubcode,
                             ErrorData errorData,
                             String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.type = type;
        this.code = code;
        this.errorSubcode = errorSubcode;
        this.errorData = errorData;
    }

    public MetaApiException(HttpStatus httpStatus, String type, int code, String message) {
        this(httpStatus, type, code, null, null, message);
    }
}
