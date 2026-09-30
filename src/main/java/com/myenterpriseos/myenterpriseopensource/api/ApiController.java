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

    @PostMapping("/companies/{companyId}/products") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') and @tenantGuard.company(#p0)")
    public ProductResponse createProduct(@PathVariable Long companyId, @Valid @RequestBody ProductRequest body) {
        return service.createProduct(companyId, body);
    }
    @GetMapping("/companies/{companyId}/products")
    @PreAuthorize("@tenantGuard.company(#p0)")
    public List<ProductResponse> products(@PathVariable Long companyId) { return service.products(companyId); }

    @PostMapping("/companies/{companyId}/warehouses") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER') and @tenantGuard.company(#p0)")
    public WarehouseResponse createWarehouse(@PathVariable Long companyId, @Valid @RequestBody WarehouseRequest body) {
        return service.createWarehouse(companyId, body);
    }
    @GetMapping("/companies/{companyId}/warehouses")
    @PreAuthorize("@tenantGuard.company(#p0)")
    public List<WarehouseResponse> warehouses(@PathVariable Long companyId) { return service.warehouses(companyId); }

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
}
