package io.orion.identity.infrastructure.security;

import io.orion.identity.application.port.TokenService;
import io.orion.identity.domain.User;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
class JwtTokenService implements TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder encoder;
    private final IdentityProperties.Jwt props;
    private final Clock clock;

    JwtTokenService(JwtEncoder encoder, IdentityProperties props, Clock clock) {
        this.encoder = encoder; this.props = props.jwt(); this.clock = clock;
    }

    @Override public AccessToken issueAccessToken(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTokenTtl()))
                .subject(user.id().toString())
                .id(UUID.randomUUID().toString())
                .claim("tenant_id", user.tenantId().toString())
                .claim("email", user.email())
                .claim("roles", List.copyOf(user.roles()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AccessToken(token, props.accessTokenTtl().toSeconds());
    }

    @Override public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override public String hashRefreshToken(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override public Duration refreshTokenTtl() { return props.refreshTokenTtl(); }
}
