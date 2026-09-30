package com.myenterpriseos.myenterpriseopensource.api;

import com.myenterpriseos.myenterpriseopensource.dto.ApiDtos.*;
import com.myenterpriseos.myenterpriseopensource.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final BusinessService service;

    public ApiController(BusinessService service) { this.service = service; }

    @PostMapping("/companies/{companyId}/customers") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SALES') and @tenantGuard.company(#p0)")
    public CustomerResponse createCustomer(@PathVariable Long companyId, @Valid @RequestBody CustomerRequest body) {
        return service.createCustomer(companyId, body);
    }
    @GetMapping("/companies/{companyId}/customers")
    @PreAuthorize("@tenantGuard.company(#p0)")
    public PageResponse<CustomerResponse> customers(@PathVariable Long companyId,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.customers(companyId, q, page, size); }
    @PutMapping("/companies/{companyId}/customers/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES') and @tenantGuard.company(#p0)")
    public CustomerResponse updateCustomer(@PathVariable Long companyId, @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequest body) { return service.updateCustomer(companyId, customerId, body); }
    @PostMapping("/companies/{companyId}/customers/{customerId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','SALES') and @tenantGuard.company(#p0)")
    public CustomerResponse deactivateCustomer(@PathVariable Long companyId, @PathVariable Long customerId) {
        return service.deactivateCustomer(companyId, customerId);
    }

    @PostMapping("/companies/{companyId}/products") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') and @tenantGuard.company(#p0)")
    public ProductResponse createProduct(@PathVariable Long companyId, @Valid @RequestBody ProductRequest body) {
        return service.createProduct(companyId, body);
    }
    @GetMapping("/companies/{companyId}/products")
    @PreAuthorize("@tenantGuard.company(#p0)")
    public List<ProductResponse> products(@PathVariable Long companyId) { return service.products(companyId); }
    @GetMapping("/companies/{companyId}/products/search")
    @PreAuthorize("@tenantGuard.company(#p0)")
    public PageResponse<ProductResponse> productPage(@PathVariable Long companyId,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.productPage(companyId, q, page, size); }
    @PutMapping("/companies/{companyId}/products/{productId}")
    @PreAuthorize("hasRole('ADMIN') and @tenantGuard.company(#p0)")
    public ProductResponse updateProduct(@PathVariable Long companyId, @PathVariable Long productId,
            @Valid @RequestBody ProductRequest body) { return service.updateProduct(companyId, productId, body); }
    @PostMapping("/companies/{companyId}/products/{productId}/deactivate")
    @PreAuthorize("hasRole('ADMIN') and @tenantGuard.company(#p0)")
    public ProductResponse deactivateProduct(@PathVariable Long companyId, @PathVariable Long productId) {
        return service.deactivateProduct(companyId, productId);
    }

    @PostMapping("/companies/{companyId}/warehouses") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.company(#p0)")
    public WarehouseResponse createWarehouse(@PathVariable Long companyId, @Valid @RequestBody WarehouseRequest body) {
        return service.createWarehouse(companyId, body);
    }
    @GetMapping("/companies/{companyId}/warehouses")
    @PreAuthorize("@tenantGuard.company(#p0)")
    public List<WarehouseResponse> warehouses(@PathVariable Long companyId) { return service.warehouses(companyId); }
    @PutMapping("/companies/{companyId}/warehouses/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.company(#p0)")
    public WarehouseResponse updateWarehouse(@PathVariable Long companyId, @PathVariable Long warehouseId,
            @Valid @RequestBody WarehouseRequest body) { return service.updateWarehouse(companyId, warehouseId, body); }
    @PostMapping("/companies/{companyId}/warehouses/{warehouseId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.company(#p0)")
    public WarehouseResponse deactivateWarehouse(@PathVariable Long companyId, @PathVariable Long warehouseId) {
        return service.deactivateWarehouse(companyId, warehouseId);
    }

    @PostMapping("/warehouses/{warehouseId}/inventory") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.warehouse(#p0)")
    public InventoryResponse addStock(@PathVariable Long warehouseId, @Valid @RequestBody StockRequest body) {
        return service.addStock(warehouseId, body);
    }
    @GetMapping("/warehouses/{warehouseId}/inventory")
    @PreAuthorize("@tenantGuard.warehouse(#p0)")
    public List<InventoryResponse> inventory(@PathVariable Long warehouseId) { return service.inventory(warehouseId); }
    @PostMapping("/inventory/{inventoryId}/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.inventory(#p0)")
    public InventoryResponse adjust(@PathVariable Long inventoryId, @Valid @RequestBody AdjustmentRequest body) {
        return service.adjust(inventoryId, body);
    }
    @PostMapping("/inventory/{inventoryId}/stocktake")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.inventory(#p0)")
    public InventoryResponse stocktake(@PathVariable Long inventoryId, @Valid @RequestBody StocktakeRequest body) {
        return service.stocktake(inventoryId, body);
    }
    @PostMapping("/companies/{companyId}/transfers") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.company(#p0)")
    public TransferResponse transfer(@PathVariable Long companyId, @Valid @RequestBody TransferRequest body) {
        return service.transfer(companyId, body);
    }
    @GetMapping("/inventory/{inventoryId}/movements")
    @PreAuthorize("@tenantGuard.inventory(#p0)")
    public List<MovementResponse> movements(@PathVariable Long inventoryId) { return service.movements(inventoryId); }

    @PostMapping("/companies/{companyId}/orders") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SALES') and @tenantGuard.company(#p0)")
    public OrderResponse createOrder(@PathVariable Long companyId, @Valid @RequestBody OrderRequest body) {
        return service.createOrder(companyId, body);
    }
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("@tenantGuard.order(#p0)")
    public OrderResponse order(@PathVariable Long orderId) { return service.order(orderId); }
    @GetMapping("/companies/{companyId}/orders")
    @PreAuthorize("@tenantGuard.company(#p0)")
    public List<OrderResponse> orders(@PathVariable Long companyId) { return service.orders(companyId); }
    @GetMapping("/companies/{companyId}/orders/search")
    @PreAuthorize("@tenantGuard.company(#p0)")
    public PageResponse<OrderResponse> orderPage(@PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.orderPage(companyId, page, size);
    }
    @GetMapping("/orders/{orderId}/shipments")
    @PreAuthorize("@tenantGuard.order(#p0)")
    public List<ShipmentResponse> shipments(@PathVariable Long orderId) { return service.shipments(orderId); }
    @PostMapping("/orders/{orderId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','SALES') and @tenantGuard.order(#p0)")
    public OrderResponse confirm(@PathVariable Long orderId, @Valid @RequestBody ConfirmRequest body) {
        return service.confirm(orderId, body);
    }
    @PostMapping("/orders/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','SALES') and @tenantGuard.order(#p0)")
    public OrderResponse cancel(@PathVariable Long orderId) { return service.cancel(orderId); }
    @PostMapping("/orders/{orderId}/shipments") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.order(#p0)")
    public ShipmentResponse ship(@PathVariable Long orderId, @Valid @RequestBody ShipmentRequest body) {
        return service.ship(orderId, body);
    }
    @PostMapping("/orders/{orderId}/returns") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.order(#p0)")
    public ReturnResponse returnStock(@PathVariable Long orderId, @Valid @RequestBody ReturnRequest body) {
        return service.returnStock(orderId, body);
    }
}
