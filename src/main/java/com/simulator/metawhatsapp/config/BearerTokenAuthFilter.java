package com.simulator.metawhatsapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulator.metawhatsapp.dto.response.ErrorDetail;
import com.simulator.metawhatsapp.dto.response.MetaErrorResponse;
import com.simulator.metawhatsapp.exception.MetaApiException;
import com.simulator.metawhatsapp.generator.FbTraceIdGenerator;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
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

    private final SimulatorProperties properties;
    private final ObjectMapper objectMapper;
    private final FbTraceIdGenerator fbTraceIdGenerator;

    public BearerTokenAuthFilter(SimulatorProperties properties,
                                 ObjectMapper objectMapper,
                                 FbTraceIdGenerator fbTraceIdGenerator) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fbTraceIdGenerator = fbTraceIdGenerator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Webhook verification (GET /webhook) authenticates via hub.verify_token, not Bearer.
        return request.getRequestURI().startsWith("/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || header.isBlank()) {
            log.warn("Rejected request to {} - missing Authorization header", request.getRequestURI());
            writeError(response, new MetaApiException(
                    HttpStatus.BAD_REQUEST, "OAuthException", 2500,
                    "An active access token must be used to query information about the current user."));
            return;
        }

        if (!header.startsWith(BEARER_PREFIX) || header.substring(BEARER_PREFIX.length()).isBlank()) {
            log.warn("Rejected request to {} - malformed Authorization header", request.getRequestURI());
            writeError(response, invalidTokenException());
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (!properties.auth().validTokens().contains(token)) {
            log.warn("Rejected request to {} - unrecognized access token", request.getRequestURI());
            writeError(response, invalidTokenException());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private MetaApiException invalidTokenException() {
        return new MetaApiException(HttpStatus.UNAUTHORIZED, "OAuthException", 190, "Invalid OAuth access token.");
    }

    private void writeError(HttpServletResponse response, MetaApiException ex) throws IOException {
        response.setStatus(ex.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("WWW-Authenticate",
                "OAuth \"Facebook Platform\" \"invalid_token\" \"Invalid OAuth access token.\"");
        response.setHeader("X-FB-Trace-Id", fbTraceIdGenerator.generate());

        ErrorDetail detail = new ErrorDetail(
                ex.getMessage(), ex.getType(), ex.getCode(),
                ex.getErrorData(), ex.getErrorSubcode(), fbTraceIdGenerator.generate());
        response.getWriter().write(objectMapper.writeValueAsString(MetaErrorResponse.of(detail)));
    }
}