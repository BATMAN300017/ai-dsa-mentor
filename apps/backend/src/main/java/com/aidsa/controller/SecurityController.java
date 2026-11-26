package com.aidsa.controller;

import com.aidsa.dto.SecurityHandshakeResponse;
import com.aidsa.exception.ApiException;
import com.aidsa.security.LocalTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues a scoped ephemeral UI token to allowlisted localhost browser sessions.
 * Rejects browser top-level navigations, script tags, and requests lacking the custom
 * client header so arbitrary web pages cannot retrieve a mutating token.
 */
@RestController
@RequestMapping("/api/v1/security")
public class SecurityController {

    private final LocalTokenService localTokenService;

    public SecurityController(LocalTokenService localTokenService) {
        this.localTokenService = localTokenService;
    }

    @GetMapping("/handshake")
    public ResponseEntity<SecurityHandshakeResponse> handshake(HttpServletRequest request) {
        String client = request.getHeader(LocalTokenService.CLIENT_HEADER);
        if (!LocalTokenService.EXPECTED_WEB_CLIENT.equals(client)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Missing required local UI client header");
        }

        String secFetchMode = request.getHeader("Sec-Fetch-Mode");
        if (secFetchMode != null && "navigate".equalsIgnoreCase(secFetchMode.trim())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Navigation requests cannot perform token handshake");
        }

        String secFetchDest = request.getHeader("Sec-Fetch-Dest");
        if (secFetchDest != null && !"empty".equalsIgnoreCase(secFetchDest.trim())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unexpected fetch destination for token handshake");
        }

        String token = localTokenService.issueUiHandshakeToken();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new SecurityHandshakeResponse(LocalTokenService.TOKEN_HEADER, token));
    }
}
