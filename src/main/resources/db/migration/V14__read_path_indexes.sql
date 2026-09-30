CREATE INDEX idx_sales_order_company_id ON sales_order (company_id, id);
CREATE INDEX idx_order_item_order_id ON order_item (order_id, id);
CREATE INDEX idx_inventory_warehouse_id ON inventory (warehouse_id, id);
CREATE INDEX idx_reservation_inventory_status ON stock_reservation (inventory_id, status);
CREATE INDEX idx_shipment_order_id ON shipment (order_id, id);
CREATE INDEX idx_movement_inventory_id ON inventory_movement (inventory_id, id);
