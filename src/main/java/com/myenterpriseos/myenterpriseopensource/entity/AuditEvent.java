package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "actor_user_id")
    private Long actorUserId;
    @Column(name = "username")
    private String username;
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(name = "http_method", nullable = false, length = 8)
    private String httpMethod;
    @Column(name = "route", nullable = false, length = 255)
    private String route;
    @Column(name = "path", nullable = false, length = 512)
    private String path;
    @Column(name = "status_code", nullable = false)
    private int statusCode;
    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    public Long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public Long getCompanyId() { return companyId; }
    public Long getActorUserId() { return actorUserId; }
    public String getUsername() { return username; }
    public String getRequestId() { return requestId; }
    public String getHttpMethod() { return httpMethod; }
    public String getRoute() { return route; }
    public String getPath() { return path; }
    public int getStatusCode() { return statusCode; }
    public long getDurationMs() { return durationMs; }
    public void setOccurredAt(Instant value) { occurredAt = value; }
    public void setCompanyId(Long value) { companyId = value; }
    public void setActorUserId(Long value) { actorUserId = value; }
    public void setUsername(String value) { username = value; }
    public void setRequestId(String value) { requestId = value; }
    public void setHttpMethod(String value) { httpMethod = value; }
    public void setRoute(String value) { route = value; }
    public void setPath(String value) { path = value; }
    public void setStatusCode(int value) { statusCode = value; }
    public void setDurationMs(long value) { durationMs = value; }
}
