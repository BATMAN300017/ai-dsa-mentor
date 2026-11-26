package com.aidsa.security;

import com.aidsa.config.AppProperties;
import com.aidsa.dto.ApiErrorResponse;
import com.aidsa.util.LoopbackValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Enforces loopback Host validation (DNS-rebinding mitigation), strict local Origin allowlisting,
 * and local token enforcement on mutating API operations.
 */
@Component
public class LocalOriginFilter extends OncePerRequestFilter {

    private final AppProperties properties;
    private final LocalTokenService localTokenService;
    private final ObjectMapper objectMapper;

    public LocalOriginFilter(
            AppProperties properties,
            LocalTokenService localTokenService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.localTokenService = localTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String host = request.getHeader("Host");
        if (!LoopbackValidator.isLoopbackHostHeader(host)) {
            writeForbidden(response, request, "Non-loopback Host header rejected");
            return;
        }

        if (request.getRequestURI().startsWith("/api/")) {
            List<String> allowedOrigins = properties.security() != null
                    ? properties.security().allowedOrigins()
                    : List.of();
            List<String> allowedExtIds = properties.security() != null
                    ? properties.security().allowedExtensionIds()
                    : List.of();

            String origin = request.getHeader("Origin");
            if (!LoopbackValidator.isAllowedOrigin(origin, allowedOrigins, allowedExtIds)) {
                writeForbidden(response, request, "Origin is not in local allowlist");
                return;
            }

            boolean requireToken = properties.security() != null
                    && properties.security().requireLocalTokenForMutations();
            if (requireToken && isMutatingMethod(request.getMethod())) {
                String token = request.getHeader(LocalTokenService.TOKEN_HEADER);
                if (!localTokenService.isValid(token)) {
                    writeError(response, request, HttpStatus.UNAUTHORIZED, "Missing or invalid local session token");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isMutatingMethod(String method) {
        return !HttpMethod.GET.matches(method)
                && !HttpMethod.HEAD.matches(method)
                && !HttpMethod.OPTIONS.matches(method);
    }

    private void writeForbidden(HttpServletResponse response, HttpServletRequest request, String message)
            throws IOException {
        writeError(response, request, HttpStatus.FORBIDDEN, message);
    }

    private void writeError(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
