CREATE TABLE inventory_movement
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,

    inventory_id    BIGINT      NOT NULL,

    movement_type   VARCHAR(50) NOT NULL,

    quantity_change INT         NOT NULL,

    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    reason          VARCHAR(255),

    CONSTRAINT fk_movement_inventory
        FOREIGN KEY (inventory_id)
            REFERENCES inventory (id),

    CONSTRAINT chk_movement_type
        CHECK (
            movement_type IN (
                              'INITIAL_STOCK',
                              'ADJUSTMENT_IN',
                              'ADJUSTMENT_OUT',
                              'SHIPMENT',
                              'TRANSFER_IN',
                              'TRANSFER_OUT'
                )
            ),

    CONSTRAINT chk_quantity_change
        CHECK (quantity_change <> 0)
);