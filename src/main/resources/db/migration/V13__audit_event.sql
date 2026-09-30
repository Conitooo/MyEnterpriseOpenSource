CREATE TABLE audit_event
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    occurred_at TIMESTAMP(6) NOT NULL,
    company_id BIGINT,
    actor_user_id BIGINT,
    username VARCHAR(255),
    request_id VARCHAR(36) NOT NULL,
    http_method VARCHAR(8) NOT NULL,
    route VARCHAR(255) NOT NULL,
    path VARCHAR(512) NOT NULL,
    status_code INT NOT NULL,
    duration_ms BIGINT NOT NULL
);

CREATE INDEX idx_audit_company_id ON audit_event (company_id, id);
CREATE INDEX idx_audit_request_id ON audit_event (request_id);
