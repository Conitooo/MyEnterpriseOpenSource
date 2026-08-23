CREATE TABLE app_user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,

    username      VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    role          VARCHAR(50)  NOT NULL,

    company_id    BIGINT       NOT NULL,

    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_company
        FOREIGN KEY (company_id)
            REFERENCES company (id),

    CONSTRAINT chk_user_role
        CHECK (
            role IN (
                     'ADMIN',
                     'WAREHOUSE_MANAGER',
                     'SALES',
                     'VIEWER'
                )
            ),

    CONSTRAINT uk_user_company_username
        UNIQUE (company_id, username)
);
