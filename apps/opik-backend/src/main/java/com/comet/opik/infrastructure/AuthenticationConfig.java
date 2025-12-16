package com.comet.opik.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AuthenticationConfig {

    public record UrlConfig(@Valid @JsonProperty @NotNull String url) {
    }

    @Data
    public static class LocalAuthConfig {
        @Valid @JsonProperty
        private boolean enabled;

        @Valid @JsonProperty
        private int sessionDurationDays = 7;

        @Valid @JsonProperty
        private String sessionSecret;

        @Valid @JsonProperty
        private List<LocalUser> users;
    }

    @Data
    public static class LocalUser {
        @Valid @JsonProperty
        private String username;

        @Valid @JsonProperty
        private String password;
    }

    @Valid @JsonProperty
    private boolean enabled;

    @Valid @JsonProperty
    private int apiKeyResolutionCacheTTLInSec;

    @Valid @JsonProperty
    private UrlConfig reactService;

    @Valid @JsonProperty
    private LocalAuthConfig localAuth;
}
