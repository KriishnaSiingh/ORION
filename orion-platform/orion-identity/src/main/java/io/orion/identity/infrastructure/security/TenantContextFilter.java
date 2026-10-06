package io.orion.identity.infrastructure.security;

import io.orion.shared.tenant.CurrentUser;
import io.orion.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
                Jwt jwt = auth.getToken();
                List<String> roles = jwt.getClaimAsStringList("roles");
                TenantContext.set(new CurrentUser(
                        UUID.fromString(jwt.getSubject()),
                        UUID.fromString(jwt.getClaimAsString("tenant_id")),
                        jwt.getClaimAsString("email"),
                        roles == null ? Set.of() : Set.copyOf(roles)));
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
