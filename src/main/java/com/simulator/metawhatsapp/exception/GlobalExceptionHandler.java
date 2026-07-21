package com.simulator.metawhatsapp.exception;

import com.simulator.metawhatsapp.dto.response.ErrorData;
import com.simulator.metawhatsapp.dto.response.ErrorDetail;
import com.simulator.metawhatsapp.dto.response.MetaErrorResponse;
import com.simulator.metawhatsapp.generator.FbTraceIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final FbTraceIdGenerator fbTraceIdGenerator;

    @ExceptionHandler(MetaApiException.class)
    public ResponseEntity<MetaErrorResponse> handleMetaApiException(MetaApiException ex) {
        log.warn("Rejecting request: type={} code={} message={}", ex.getType(), ex.getCode(), ex.getMessage());
        return build(ex.getHttpStatus(), ex.getType(), ex.getCode(), ex.getErrorSubcode(),
                ex.getErrorData(), ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MetaErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Rejecting request: malformed JSON body - {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "OAuthException", 100, null, null,
                "(#100) Invalid parameter - request body is not valid JSON");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<MetaErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Rejecting request: unsupported HTTP method - {}", ex.getMessage());
        return build(HttpStatus.METHOD_NOT_ALLOWED, "GraphMethodException", 100, null, null,
                "(#100) " + ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<MetaErrorResponse> handleNotFound(NoResourceFoundException ex) {
        log.warn("Rejecting request: endpoint not found - {}", ex.getResourcePath());
        return build(
                HttpStatus.NOT_FOUND,
                "GraphMethodException",
                100,
                null,
                null,
                "Unsupported get request. Object with ID '%s' does not exist, cannot be loaded due to missing permissions, or does not support this operation."
                        .formatted(ex.getResourcePath())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MetaErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error handling request", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Exception", 1, null, null, "An unknown error occurred");
    }

    private ResponseEntity<MetaErrorResponse> build(HttpStatus status, String type, int code,
                                                    Integer subcode, ErrorData errorData, String message) {
        String traceId = fbTraceIdGenerator.generate();
        ErrorDetail detail = new ErrorDetail(message, type, code, errorData, subcode, traceId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-FB-Trace-Id", traceId);
        if (status == HttpStatus.UNAUTHORIZED) {
            headers.set("WWW-Authenticate",
                    "OAuth \"Facebook Platform\" \"invalid_token\" \"Invalid OAuth access token.\"");
        }

        return ResponseEntity.status(status).headers(headers).body(MetaErrorResponse.of(detail));
    }
}