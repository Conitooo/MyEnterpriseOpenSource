package com.myenterpriseos.myenterpriseopensource.service;

import com.myenterpriseos.myenterpriseopensource.dto.ApiDtos.*;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import com.myenterpriseos.myenterpriseopensource.repository.ProductRepository;
import com.myenterpriseos.myenterpriseopensource.repository.SalesOrderRepository;
import com.myenterpriseos.myenterpriseopensource.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("local")
@Order(1)
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private final CompanyRepository companies;
    private final ProductRepository products;
    private final WarehouseRepository warehouses;
    private final SalesOrderRepository orders;
    private final BusinessService business;

    public DemoDataSeeder(CompanyRepository companies, ProductRepository products,
                          WarehouseRepository warehouses, SalesOrderRepository orders,
                          BusinessService business) {
        this.companies = companies;
        this.products = products;
        this.warehouses = warehouses;
        this.orders = orders;
        this.business = business;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Company company : companies.findAll()) {
            Long companyId = company.getId();
            if (!products.findByCompanyId(companyId).isEmpty() ||
                    !warehouses.findByCompanyId(companyId).isEmpty() ||
                    !orders.findByCompanyId(companyId).isEmpty()) {
                log.info("demo_data_skipped companyId={} reason=existing_operational_data", companyId);
                continue;
            }
            seed(companyId);
            log.info("demo_data_seeded companyId={} products=3 warehouses=2 orders=4", companyId);
        }
    }

    private void seed(Long companyId) {
        CustomerResponse customer = business.createCustomer(companyId,
                new CustomerRequest("Cliente demo", "demo@example.invalid", null));
        DeliveryAddress address = new DeliveryAddress("Cliente demo", "Calle Demo 1", "Madrid", "28001", "España");
        ProductResponse keyboard = business.createProduct(companyId,
                new ProductRequest("Teclado mecánico", "DEMO-TEC-001", new BigDecimal("59.90"), "EUR"));
        ProductResponse mouse = business.createProduct(companyId,
                new ProductRequest("Ratón inalámbrico", "DEMO-RAT-002", new BigDecimal("29.90"), "EUR"));
        ProductResponse monitor = business.createProduct(companyId,
                new ProductRequest("Monitor 27 pulgadas", "DEMO-MON-003", new BigDecimal("199.00"), "EUR"));

        WarehouseResponse central = business.createWarehouse(companyId,
                new WarehouseRequest("DEMO-CENTRAL", "Almacén central"));
        WarehouseResponse north = business.createWarehouse(companyId,
                new WarehouseRequest("DEMO-NORTE", "Almacén norte"));

        InventoryResponse keyboardCentral = business.addStock(central.id(), new StockRequest(keyboard.id(), 24));
        business.addStock(north.id(), new StockRequest(keyboard.id(), 8));
        business.addStock(central.id(), new StockRequest(mouse.id(), 35));
        InventoryResponse mouseNorth = business.addStock(north.id(), new StockRequest(mouse.id(), 16));
        InventoryResponse monitorCentral = business.addStock(central.id(), new StockRequest(monitor.id(), 12));
        business.adjust(keyboardCentral.id(), new AdjustmentRequest(3, "Reposición de demostración"));

        business.createOrder(companyId, new OrderRequest(customer.id(), address, List.of(
                new OrderLineRequest(keyboard.id(), 2), new OrderLineRequest(mouse.id(), 1))));

        OrderResponse confirmed = business.createOrder(companyId, new OrderRequest(customer.id(), address, List.of(
                new OrderLineRequest(monitor.id(), 2), new OrderLineRequest(mouse.id(), 3))));
        business.confirm(confirmed.id(), new ConfirmRequest(List.of(
                new AllocationRequest(confirmed.items().get(0).id(), monitorCentral.id(), 2),
                new AllocationRequest(confirmed.items().get(1).id(), mouseNorth.id(), 3))));

        OrderResponse shipped = business.createOrder(companyId, new OrderRequest(customer.id(), address, List.of(
                new OrderLineRequest(keyboard.id(), 3))));
        business.confirm(shipped.id(), new ConfirmRequest(List.of(
                new AllocationRequest(shipped.items().get(0).id(), keyboardCentral.id(), 3))));
        business.ship(shipped.id(), new ShipmentRequest(central.id(), "Demo Carrier", "DEMO-001", List.of(
                new ShipmentLineRequest(shipped.items().get(0).id(), 3))));

        OrderResponse partial = business.createOrder(companyId, new OrderRequest(customer.id(), address, List.of(
                new OrderLineRequest(mouse.id(), 6))));
        business.confirm(partial.id(), new ConfirmRequest(List.of(
                new AllocationRequest(partial.items().get(0).id(), mouseNorth.id(), 6))));
        business.ship(partial.id(), new ShipmentRequest(north.id(), "Demo Carrier", "DEMO-002", List.of(
                new ShipmentLineRequest(partial.items().get(0).id(), 2))));
    }
}
