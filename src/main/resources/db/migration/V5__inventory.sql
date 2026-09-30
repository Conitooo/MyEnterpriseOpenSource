CREATE TABLE inventory
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,

    product_id   BIGINT NOT NULL,

    warehouse_id BIGINT NOT NULL,

    quantity     INT    NOT NULL DEFAULT 0,

    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id)
            REFERENCES product (id),

    CONSTRAINT fk_inventory_warehouse
        FOREIGN KEY (warehouse_id)
            REFERENCES warehouse (id),

    CONSTRAINT chk_inventory_quantity
        CHECK (quantity >= 0),

    CONSTRAINT uk_inventory_product_warehouse
        UNIQUE (product_id, warehouse_id)
);
