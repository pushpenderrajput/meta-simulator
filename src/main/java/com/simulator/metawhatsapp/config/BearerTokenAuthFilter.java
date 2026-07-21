package com.simulator.metawhatsapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulator.metawhatsapp.dto.response.ErrorDetail;
import com.simulator.metawhatsapp.dto.response.MetaErrorResponse;
import com.simulator.metawhatsapp.exception.MetaApiException;
import com.simulator.metawhatsapp.generator.FbTraceIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class BearerTokenAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;
    private final FbTraceIdGenerator fbTraceIdGenerator;

    public BearerTokenAuthFilter(ObjectMapper objectMapper,
                                 FbTraceIdGenerator fbTraceIdGenerator) {
        this.objectMapper = objectMapper;
        this.fbTraceIdGenerator = fbTraceIdGenerator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Webhook verification / callbacks bypass bearer authentication
        return request.getRequestURI().startsWith("/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // 1. Ensure Authorization header exists
        if (header == null || header.isBlank()) {
            log.warn("Rejected request to {} - missing Authorization header", request.getRequestURI());
            writeError(response, new MetaApiException(
                    HttpStatus.BAD_REQUEST, "OAuthException", 2500,
                    "An active access token must be used to query information about the current user."));
            return;
        }

        // 2. Ensure format is 'Bearer <token>' with a non-empty token string
        if (!header.startsWith(BEARER_PREFIX) || header.substring(BEARER_PREFIX.length()).isBlank()) {
            log.warn("Rejected request to {} - malformed Authorization header", request.getRequestURI());
            writeError(response, new MetaApiException(
                    HttpStatus.UNAUTHORIZED, "OAuthException", 190, "Invalid OAuth access token."));
            return;
        }

        // ANY token is accepted - allow request through
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, MetaApiException ex) throws IOException {
        response.setStatus(ex.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("WWW-Authenticate",
                "OAuth \"Facebook Platform\" \"invalid_token\" \"Invalid OAuth access token.\"");

        String traceId = fbTraceIdGenerator.generate();
        response.setHeader("X-FB-Trace-Id", traceId);

        ErrorDetail detail = new ErrorDetail(
                ex.getMessage(), ex.getType(), ex.getCode(),
                ex.getErrorData(), ex.getErrorSubcode(), traceId);
        response.getWriter().write(objectMapper.writeValueAsString(MetaErrorResponse.of(detail)));
    }
}