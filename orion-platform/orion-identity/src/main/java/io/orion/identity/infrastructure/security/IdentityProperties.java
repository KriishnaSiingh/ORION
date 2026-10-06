package io.orion.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

@ConfigurationProperties(prefix = "orion.identity")
public record IdentityProperties(Jwt jwt) {
    public record Jwt(String secret,
                      @DefaultValue("orion-platform") String issuer,
                      @DefaultValue("15m") Duration accessTokenTtl,
                      @DefaultValue("7d") Duration refreshTokenTtl) {}
}
