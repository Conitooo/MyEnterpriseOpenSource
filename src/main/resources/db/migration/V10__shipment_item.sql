CREATE TABLE shipment_item
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,

    shipment_id   BIGINT NOT NULL,

    order_item_id BIGINT NOT NULL,

    quantity      INT    NOT NULL,

    CONSTRAINT fk_shipment_item_shipment
        FOREIGN KEY (shipment_id)
            REFERENCES shipment (id),

    CONSTRAINT fk_shipment_item_order_item
        FOREIGN KEY (order_item_id)
            REFERENCES order_item (id),

    CONSTRAINT chk_shipment_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT uk_shipment_order_item
        UNIQUE (shipment_id, order_item_id)
);
