CREATE TABLE sales_order
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,

    company_id   BIGINT      NOT NULL,

    status       VARCHAR(50) NOT NULL DEFAULT 'DRAFT',

    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    confirmed_at TIMESTAMP,

    cancelled_at TIMESTAMP,

    CONSTRAINT fk_order_company
        FOREIGN KEY (company_id)
            REFERENCES company (id),

    CONSTRAINT chk_order_status
        CHECK (
            status IN (
                       'DRAFT',
                       'CONFIRMED',
                       'CANCELLED',
                       'SHIPPED'
                )
            )
);