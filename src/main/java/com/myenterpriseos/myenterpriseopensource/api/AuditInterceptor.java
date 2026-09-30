package com.myenterpriseos.myenterpriseopensource.api;

import com.myenterpriseos.myenterpriseopensource.entity.AuditEvent;
import com.myenterpriseos.myenterpriseopensource.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class AuditInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);
    private static final Set<String> MUTATIONS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditService audit;

    public AuditInterceptor(AuditService audit) { this.audit = audit; }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        if (!MUTATIONS.contains(request.getMethod())) return;
        // Invalid logins appear in the HTTP log, without letting anonymous traffic fill the audit table.
        if (request.getRequestURI().equals("/api/auth/login") && response.getStatus() != 200) return;
        AuditEvent event = new AuditEvent();
        event.setOccurredAt(Instant.now());
        Object requestId = request.getAttribute(RequestTraceFilter.REQUEST_ID);
        event.setRequestId(requestId instanceof String id ? id : UUID.randomUUID().toString());
        event.setHttpMethod(request.getMethod());
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        event.setRoute(trim(pattern == null ? request.getRequestURI() : pattern.toString(), 255));
        event.setPath(trim(request.getRequestURI(), 512));
        event.setStatusCode(exception == null ? response.getStatus() : 500);
        Object started = request.getAttribute(RequestTraceFilter.START_NANOS);
        event.setDurationMs(started instanceof Long nanos
                ? TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanos) : 0);
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            try { event.setActorUserId(Long.valueOf(token.getToken().getSubject())); }
            catch (NumberFormatException ignored) { /* Authentication has already validated the subject. */ }
            Object company = token.getToken().getClaim("company_id");
            if (company instanceof Number id) event.setCompanyId(id.longValue());
            event.setUsername(token.getName());
        } else {
            Object company = request.getAttribute("audit.companyId");
            if (company instanceof Long id) event.setCompanyId(id);
            Object username = request.getAttribute("audit.username");
            if (username instanceof String name) event.setUsername(trim(name, 255));
        }
        try { audit.record(event); }
        catch (RuntimeException ex) {
            log.error("audit_persist_failed requestId={} route={}", event.getRequestId(), event.getRoute(), ex);
        }
    }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
