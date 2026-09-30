package com.myenterpriseos.myenterpriseopensource;

import com.myenterpriseos.myenterpriseopensource.dto.ApiDtos.*;
import com.myenterpriseos.myenterpriseopensource.service.BusinessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MySqlConcurrencyTests {
    @Container static final MySQLContainer mysql = new MySQLContainer("mysql:8.4")
            .withDatabaseName("enterprise_test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired BusinessService service;

    @Test
    void concurrentConfirmationsCannotOversellOnMySql() throws Exception {
        Long company = service.createCompany(new CompanyRequest("Concurrent Co")).id();
        Long customer = service.createCustomer(company, new CustomerRequest("Buyer", null, null)).id();
        Long product = service.createProduct(company,
                new ProductRequest("Widget", "W-1", BigDecimal.ONE, "EUR")).id();
        Long warehouse = service.createWarehouse(company, new WarehouseRequest("W", "Main")).id();
        Long stock = service.addStock(warehouse, new StockRequest(product, 5)).id();
        DeliveryAddress address = new DeliveryAddress("Buyer", "Street 1", "Madrid", "28001", "Spain");
        OrderResponse first = service.createOrder(company,
                new OrderRequest(customer, address, List.of(new OrderLineRequest(product, 4))));
        OrderResponse second = service.createOrder(company,
                new OrderRequest(customer, address, List.of(new OrderLineRequest(product, 4))));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> confirmWhenReleased(first, stock, ready, go));
            var b = executor.submit(() -> confirmWhenReleased(second, stock, ready, go));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            go.countDown();
            int successes = (a.get(20, TimeUnit.SECONDS) ? 1 : 0) + (b.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, service.inventory(warehouse).getFirst().available());
        }
    }

    private boolean confirmWhenReleased(OrderResponse order, Long stock, CountDownLatch ready,
                                        CountDownLatch go) throws InterruptedException {
        ready.countDown();
        if (!go.await(10, TimeUnit.SECONDS)) return false;
        try {
            service.confirm(order.id(), new ConfirmRequest(List.of(
                    new AllocationRequest(order.items().getFirst().id(), stock, 4))));
            return true;
        } catch (RuntimeException expected) {
            return false;
        }
    }
}
