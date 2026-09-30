CREATE TABLE customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_customer_company FOREIGN KEY (company_id) REFERENCES company(id)
);
CREATE INDEX idx_customer_company_name ON customer(company_id, name, id);

ALTER TABLE product ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE warehouse ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE sales_order ADD COLUMN customer_id BIGINT NULL;
ALTER TABLE sales_order ADD COLUMN recipient VARCHAR(255) NULL;
ALTER TABLE sales_order ADD COLUMN delivery_street VARCHAR(255) NULL;
ALTER TABLE sales_order ADD COLUMN delivery_city VARCHAR(100) NULL;
ALTER TABLE sales_order ADD COLUMN delivery_postal_code VARCHAR(30) NULL;
ALTER TABLE sales_order ADD COLUMN delivery_country VARCHAR(100) NULL;
ALTER TABLE sales_order ADD CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id);

ALTER TABLE shipment ADD COLUMN carrier VARCHAR(100) NULL;
ALTER TABLE shipment ADD COLUMN tracking_number VARCHAR(100) NULL;

CREATE TABLE stock_transfer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    source_warehouse_id BIGINT NOT NULL,
    destination_warehouse_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transfer_company FOREIGN KEY (company_id) REFERENCES company(id),
    CONSTRAINT fk_transfer_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_transfer_source FOREIGN KEY (source_warehouse_id) REFERENCES warehouse(id),
    CONSTRAINT fk_transfer_destination FOREIGN KEY (destination_warehouse_id) REFERENCES warehouse(id),
    CONSTRAINT chk_transfer_quantity CHECK (quantity > 0)
);
CREATE INDEX idx_transfer_company_created ON stock_transfer(company_id, id);

CREATE TABLE stock_return (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_return_shipment_item FOREIGN KEY (shipment_item_id) REFERENCES shipment_item(id),
    CONSTRAINT chk_return_quantity CHECK (quantity > 0)
);
CREATE INDEX idx_return_shipment_item ON stock_return(shipment_item_id);

