package io.orion.identity.infrastructure.security;

import io.orion.identity.application.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BcryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    @Override public String hash(String raw) { return encoder.encode(raw); }
    @Override public boolean matches(String raw, String hash) { return encoder.matches(raw, hash); }
}
