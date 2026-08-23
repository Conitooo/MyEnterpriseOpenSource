CREATE TABLE product
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,

    company_id   BIGINT         NOT NULL,

    product_name VARCHAR(255)   NOT NULL,

    sku          VARCHAR(255)   NOT NULL,

    price        DECIMAL(12, 2) NOT NULL,

    currency     CHAR(3)        NOT NULL,

    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_company
        FOREIGN KEY (company_id)
            REFERENCES company (id),

    CONSTRAINT chk_product_price
        CHECK (price >= 0),

    CONSTRAINT uk_product_company_sku
        UNIQUE (company_id, sku)
);
