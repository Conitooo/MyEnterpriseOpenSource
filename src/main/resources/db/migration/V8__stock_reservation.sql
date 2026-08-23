CREATE TABLE stock_reservation
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,

    order_item_id BIGINT      NOT NULL,

    inventory_id  BIGINT      NOT NULL,

    quantity      INT         NOT NULL,

    status        VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    released_at   TIMESTAMP,

    CONSTRAINT fk_reservation_order_item
        FOREIGN KEY (order_item_id)
            REFERENCES order_item (id),

    CONSTRAINT fk_reservation_inventory
        FOREIGN KEY (inventory_id)
            REFERENCES inventory (id),

    CONSTRAINT chk_reservation_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_reservation_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'RELEASED',
                       'CONSUMED'
                )
            ),

    CONSTRAINT uk_reservation_item_inventory
        UNIQUE (order_item_id, inventory_id)
);
