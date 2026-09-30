package com.myenterpriseos.myenterpriseopensource.service;

import com.myenterpriseos.myenterpriseopensource.api.ApiException;
import com.myenterpriseos.myenterpriseopensource.entity.AuditEvent;
import com.myenterpriseos.myenterpriseopensource.repository.AuditEventRepository;
import com.myenterpriseos.myenterpriseopensource.security.TenantGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class AuditService {
    private final AuditEventRepository events;
    private final TenantGuard tenant;

    public AuditService(AuditEventRepository events, TenantGuard tenant) {
        this.events = events;
        this.tenant = tenant;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent event) { events.save(event); }

    @Transactional(readOnly = true)
    public AuditPageResponse list(int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 100)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid audit page or size");
        Long companyId = tenant.currentCompanyId();
        if (companyId == null) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        Page<AuditEvent> result = events.findByCompanyIdOrderByIdDesc(companyId, PageRequest.of(page, size));
        return new AuditPageResponse(result.map(AuditEventResponse::from).getContent(),
                result.getTotalElements(), page, size);
    }

    public record AuditEventResponse(Long id, Instant occurredAt, Long actorUserId,
                                     String username, String requestId, String httpMethod,
                                     String route, String path, int statusCode, long durationMs) {
        static AuditEventResponse from(AuditEvent event) {
            return new AuditEventResponse(event.getId(), event.getOccurredAt(), event.getActorUserId(),
                    event.getUsername(), event.getRequestId(), event.getHttpMethod(),
                    event.getRoute(), event.getPath(), event.getStatusCode(), event.getDurationMs());
        }
    }
    public record AuditPageResponse(List<AuditEventResponse> events, long total, int page, int size) {}
}
