package io.orion.audit.infrastructure.web;

import io.orion.shared.request.RequestMetadata;
import io.orion.shared.request.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class RequestMetadataFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Request-Id";
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String incoming = req.getHeader(HEADER);
        String requestId = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming : UUID.randomUUID().toString();

        RequestContext.set(new RequestMetadata(requestId, req.getRemoteAddr(), req.getHeader("User-Agent")));
        MDC.put("requestId", requestId);
        res.setHeader(HEADER, requestId);
        try {
            chain.doFilter(req, res);
        } finally {
            RequestContext.clear();
            MDC.remove("requestId");
        }
    }
}
