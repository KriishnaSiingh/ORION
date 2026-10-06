package io.orion.identity.api;

import io.orion.identity.api.dto.request.*;
import io.orion.identity.api.dto.response.*;
import io.orion.identity.application.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register-tenant")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registerTenant(@Valid @RequestBody RegisterTenantRequest r) {
        return TokenResponse.from(auth.registerTenant(new AuthService.RegisterTenantCommand(
                r.tenantName(), r.tenantSlug(), r.adminEmail(), r.adminName(), r.password())));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r) {
        return TokenResponse.from(auth.login(new AuthService.LoginCommand(r.tenantSlug(), r.email(), r.password())));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest r) {
        return TokenResponse.from(auth.refresh(r.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest r) {
        auth.logout(r.refreshToken());
    }
}
