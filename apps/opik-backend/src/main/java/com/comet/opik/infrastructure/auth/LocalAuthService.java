package com.comet.opik.infrastructure.auth;

import com.comet.opik.infrastructure.AuthenticationConfig;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Slf4j
public class LocalAuthService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_SEPARATOR = ":";

    private final AuthenticationConfig.LocalAuthConfig localAuthConfig;

    public LocalAuthService(AuthenticationConfig.LocalAuthConfig localAuthConfig) {
        this.localAuthConfig = localAuthConfig;
    }

    /**
     * Validate username and password against configured users
     */
    public boolean validateCredentials(String username, String password) {
        if (localAuthConfig == null || localAuthConfig.getUsers() == null) {
            return false;
        }

        return localAuthConfig.getUsers().stream()
                .anyMatch(user -> user.getUsername().equals(username)
                        && user.getPassword().equals(password));
    }

    /**
     * Generate a session token for the user
     * Format: username:expirationTimestamp:signature
     */
    public String generateSessionToken(String username) {
        long expirationTime = System.currentTimeMillis()
                + (long) localAuthConfig.getSessionDurationDays() * 24 * 60 * 60 * 1000;

        String payload = username + TOKEN_SEPARATOR + expirationTime;
        String signature = sign(payload);

        return Base64.getUrlEncoder().encodeToString(
                (payload + TOKEN_SEPARATOR + signature).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validate session token and extract username
     */
    public Optional<String> validateSessionToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(TOKEN_SEPARATOR);

            if (parts.length != 3) {
                log.debug("Invalid token format");
                return Optional.empty();
            }

            String username = parts[0];
            long expirationTime = Long.parseLong(parts[1]);
            String signature = parts[2];

            // Check expiration
            if (System.currentTimeMillis() > expirationTime) {
                log.debug("Token expired for user: {}", username);
                return Optional.empty();
            }

            // Verify signature
            String payload = username + TOKEN_SEPARATOR + expirationTime;
            String expectedSignature = sign(payload);

            if (!signature.equals(expectedSignature)) {
                log.debug("Invalid token signature for user: {}", username);
                return Optional.empty();
            }

            return Optional.of(username);
        } catch (Exception e) {
            log.debug("Failed to validate token", e);
            return Optional.empty();
        }
    }

    /**
     * Get session duration in seconds
     */
    public int getSessionDurationSeconds() {
        return localAuthConfig.getSessionDurationDays() * 24 * 60 * 60;
    }

    /**
     * Check if local auth is enabled
     */
    public boolean isEnabled() {
        return localAuthConfig != null && localAuthConfig.isEnabled();
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    localAuthConfig.getSessionSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to sign data", e);
        }
    }
}
