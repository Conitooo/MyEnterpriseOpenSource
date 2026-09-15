package com.myenterpriseos.myenterpriseopensource;

import com.myenterpriseos.myenterpriseopensource.dto.CompanyResponse;
import com.myenterpriseos.myenterpriseopensource.dto.CreateCompanyRequest;
import com.myenterpriseos.myenterpriseopensource.dto.ProductRequest;
import com.myenterpriseos.myenterpriseopensource.dto.ProductResponse;
import com.myenterpriseos.myenterpriseopensource.dto.WarehouseRequest;
import com.myenterpriseos.myenterpriseopensource.exception.CompanyNotFoundException;
import com.myenterpriseos.myenterpriseopensource.exception.ProductInUseException;
import com.myenterpriseos.myenterpriseopensource.exception.ProductNotFoundException;
import com.myenterpriseos.myenterpriseopensource.repository.ProductRepository;
import com.myenterpriseos.myenterpriseopensource.services.CompanyService;
import com.myenterpriseos.myenterpriseopensource.services.ProductService;
import com.myenterpriseos.myenterpriseopensource.services.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ServiceRegressionTests {

    @Autowired
    private CompanyService companyService;
    @Autowired
    private ProductService productService;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private CompanyResponse company;

    @BeforeEach
    void createCompany() {
        company = companyService.createCompany(new CreateCompanyRequest("Company " + UUID.randomUUID()));
    }

    @Test
    void deletedCompanyLookupExcludesActiveCompaniesAndTracksReactivation() {
        assertThatThrownBy(() -> companyService.findDeletedCompanyById(company.id()))
                .isInstanceOf(CompanyNotFoundException.class);

        companyService.deleteCompany(company.id());

        assertThat(companyService.findDeletedCompanyById(company.id())).isEqualTo(company);
        assertThatThrownBy(() -> companyService.findCompanyById(company.id()))
                .isInstanceOf(CompanyNotFoundException.class);

        companyService.reactivateCompany(company.id());

        assertThat(companyService.findCompanyById(company.id())).isEqualTo(company);
        assertThatThrownBy(() -> companyService.findDeletedCompanyById(company.id()))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"find", "update", "delete"})
    void productOperationsRejectDeletedCompanies(String operation) {
        ProductResponse product = productService.createProduct(company.id(), productRequest("Original"));
        companyService.deleteCompany(company.id());

        assertThatThrownBy(() -> operateOnProduct(operation, company.id(), product.id()))
                .isInstanceOf(CompanyNotFoundException.class);

        companyService.reactivateCompany(company.id());
        assertThat(productService.findProductById(company.id(), product.id())).isEqualTo(product);
    }

    @ParameterizedTest
    @ValueSource(strings = {"find", "update", "delete"})
    void productOperationsRejectProductsFromAnotherCompany(String operation) {
        CompanyResponse other = companyService.createCompany(new CreateCompanyRequest("Other " + UUID.randomUUID()));
        ProductResponse product = productService.createProduct(other.id(), productRequest("Original"));

        assertThatThrownBy(() -> operateOnProduct(operation, company.id(), product.id()))
                .isInstanceOf(ProductNotFoundException.class);

        assertThat(productService.findProductById(other.id(), product.id())).isEqualTo(product);
    }

    @Test
    void productChangesPersistAndUnreferencedProductsCanBeDeleted() {
        ProductResponse product = productService.createProduct(company.id(), productRequest("Original"));
        ProductRequest update = new ProductRequest("Updated", "SKU-UPDATED", new BigDecimal("25.50"), "USD");

        productService.updateProduct(company.id(), product.id(), update);

        ProductResponse stored = productService.findProductById(company.id(), product.id());
        assertThat(stored.name()).isEqualTo(update.name());
        assertThat(stored.sku()).isEqualTo(update.sku());
        assertThat(stored.price()).isEqualByComparingTo(update.price());
        assertThat(stored.currency()).isEqualTo(update.currency());
        assertThat(stored.companyName()).isEqualTo(company.name());
        assertThat(productService.findAllProducts(company.id())).contains(stored);

        assertThat(productService.deleteProduct(company.id(), product.id())).isEqualTo(stored);
        assertThat(productRepository.existsById(product.id())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"inventory", "order_item"})
    void referencedProductsCannotBeDeletedAndRemainIntact(String reference) {
        ProductResponse product = productService.createProduct(company.id(), productRequest("Referenced"));

        if (reference.equals("inventory")) {
            warehouseService.createWarehouse(company.id(), new WarehouseRequest("WH-1", "Main warehouse"));
            jdbc.update("""
                    INSERT INTO inventory (product_id, warehouse_id, quantity)
                    SELECT ?, id, 0 FROM warehouse WHERE company_id = ? AND code = ?
                    """, product.id(), company.id(), "WH-1");
        } else {
            jdbc.update("INSERT INTO sales_order (company_id) VALUES (?)", company.id());
            jdbc.update("""
                    INSERT INTO order_item (order_id, product_id, quantity, price, currency)
                    SELECT id, ?, 1, 10.00, 'EUR' FROM sales_order WHERE company_id = ?
                    """, product.id(), company.id());
        }

        assertThatThrownBy(() -> productService.deleteProduct(company.id(), product.id()))
                .isInstanceOf(ProductInUseException.class)
                .hasMessageContaining(product.id().toString());

        assertThat(productService.findProductById(company.id(), product.id())).isEqualTo(product);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + reference + " WHERE product_id = ?",
                Long.class, product.id())).isEqualTo(1L);
    }

    @Test
    void warehouseCreationPersistsTheCompanySelectedByTheServiceArgument() {
        var warehouse = warehouseService.createWarehouse(company.id(), new WarehouseRequest("WH-1", "Main warehouse"));

        assertThat(warehouse.code()).isEqualTo("WH-1");
        assertThat(warehouse.name()).isEqualTo("Main warehouse");
        assertThat(warehouse.companyName()).isEqualTo(company.name());
        assertThat(jdbc.queryForObject("SELECT company_id FROM warehouse WHERE company_id = ? AND code = ?",
                Long.class, company.id(), "WH-1")).isEqualTo(company.id());
    }

    @Test
    void warehouseCreationRejectsDeletedCompanies() {
        companyService.deleteCompany(company.id());

        assertThatThrownBy(() -> warehouseService.createWarehouse(company.id(), new WarehouseRequest("WH-1", "Main warehouse")))
                .isInstanceOf(CompanyNotFoundException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM warehouse WHERE company_id = ?",
                Long.class, company.id())).isZero();
    }

    private ProductRequest productRequest(String name) {
        return new ProductRequest(name, "SKU-1", new BigDecimal("10.00"), "EUR");
    }

    private void operateOnProduct(String operation, Long companyId, Long productId) {
        switch (operation) {
            case "find" -> productService.findProductById(companyId, productId);
            case "update" -> productService.updateProduct(companyId, productId, productRequest("Changed"));
            case "delete" -> productService.deleteProduct(companyId, productId);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }
}
