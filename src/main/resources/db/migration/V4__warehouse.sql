CREATE TABLE warehouse
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,

    company_id BIGINT       NOT NULL,

    code       VARCHAR(255) NOT NULL,

    name       VARCHAR(255) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_warehouse_company
        FOREIGN KEY (company_id)
            REFERENCES company (id),

    CONSTRAINT uk_warehouse_company_code
        UNIQUE (company_id, code)
);
