CREATE TABLE shipment
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,

    order_id     BIGINT      NOT NULL,

    warehouse_id BIGINT      NOT NULL,

    status       VARCHAR(50) NOT NULL DEFAULT 'CREATED',

    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    shipped_at   TIMESTAMP,

    CONSTRAINT fk_shipment_order
        FOREIGN KEY (order_id)
            REFERENCES sales_order (id),

    CONSTRAINT fk_shipment_warehouse
        FOREIGN KEY (warehouse_id)
            REFERENCES warehouse (id),

    CONSTRAINT chk_shipment_status
        CHECK (
            status IN (
                       'CREATED',
                       'SHIPPED',
                       'CANCELLED'
                )
            )
);