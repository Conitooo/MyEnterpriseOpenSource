package com.myenterpriseos.myenterpriseopensource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    public record CompanyRequest(@NotBlank @Size(max = 255) String name) {}
    public record CompanyResponse(Long id, String name) {}

    public record ProductRequest(@NotBlank @Size(max = 255) String productName,
                                 @NotBlank @Size(max = 255) String sku,
                                 @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
                                 @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) {}
    public record ProductResponse(Long id, Long companyId, String productName, String sku,
                                  BigDecimal price, String currency) {}

    public record WarehouseRequest(@NotBlank @Size(max = 255) String code,
                                   @NotBlank @Size(max = 255) String name) {}
    public record WarehouseResponse(Long id, Long companyId, String code, String name) {}

    public record StockRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {}
    public record AdjustmentRequest(@NotNull Integer quantityChange,
                                    @NotBlank @Size(max = 255) String reason) {}
    public record InventoryResponse(Long id, Long productId, Long warehouseId, int quantity, int available) {}
    public record MovementResponse(Long id, String type, int quantityChange, String reason) {}

    public record OrderLineRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {}
    public record OrderRequest(@NotEmpty List<@NotNull @Valid OrderLineRequest> items) {}
    public record OrderLineResponse(Long id, Long productId, int quantity, BigDecimal price, String currency) {}
    public record OrderResponse(Long id, Long companyId, String status, List<OrderLineResponse> items) {}

    public record AllocationRequest(@NotNull Long orderItemId, @NotNull Long inventoryId,
                                    @NotNull @Positive Integer quantity) {}
    public record ConfirmRequest(@NotEmpty List<@NotNull @Valid AllocationRequest> allocations) {}

    public record ShipmentLineRequest(@NotNull Long orderItemId, @NotNull @Positive Integer quantity) {}
    public record ShipmentRequest(@NotNull Long warehouseId,
                                  @NotEmpty List<@NotNull @Valid ShipmentLineRequest> items) {}
    public record ShipmentResponse(Long id, Long orderId, Long warehouseId, String status) {}
}
