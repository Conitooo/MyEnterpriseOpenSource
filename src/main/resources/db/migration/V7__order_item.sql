CREATE TABLE order_item
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,

    order_id   BIGINT         NOT NULL,

    product_id BIGINT         NOT NULL,

    quantity   INT            NOT NULL,

    price      DECIMAL(12, 2) NOT NULL,

    currency   CHAR(3)        NOT NULL,

    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
            REFERENCES sales_order (id),

    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id)
            REFERENCES product (id),

    CONSTRAINT chk_order_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_item_price
        CHECK (price >= 0)
);