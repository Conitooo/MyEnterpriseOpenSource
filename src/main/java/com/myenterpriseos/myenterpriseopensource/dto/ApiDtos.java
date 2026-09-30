package com.myenterpriseos.myenterpriseopensource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}
    public record PageResponse<T>(List<T> items, long total, int page, int size) {}

    public record CompanyRequest(@NotBlank @Size(max = 255) String name) {}
    public record CompanyResponse(Long id, String name) {}

    public record CustomerRequest(@NotBlank @Size(max = 255) String name,
                                  @Email @Size(max = 255) String email,
                                  @Size(max = 50) String phone) {}
    public record CustomerResponse(Long id, Long companyId, String name, String email, String phone, boolean active) {}

    public record ProductRequest(@NotBlank @Size(max = 255) String productName,
                                 @NotBlank @Size(max = 255) String sku,
                                 @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
                                 @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) {}
    public record ProductResponse(Long id, Long companyId, String productName, String sku,
                                  BigDecimal price, String currency, boolean active) {}

    public record WarehouseRequest(@NotBlank @Size(max = 255) String code,
                                   @NotBlank @Size(max = 255) String name) {}
    public record WarehouseResponse(Long id, Long companyId, String code, String name, boolean active) {}

    public record StockRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {}
    public record AdjustmentRequest(@NotNull Integer quantityChange,
                                    @NotBlank @Size(max = 255) String reason) {}
    public record TransferRequest(@NotNull Long sourceWarehouseId, @NotNull Long destinationWarehouseId,
                                  @NotNull Long productId, @NotNull @Positive Integer quantity) {}
    public record StocktakeRequest(@NotNull @PositiveOrZero Integer countedQuantity,
                                   @NotBlank @Size(max = 255) String reason) {}
    public record ReturnRequest(@NotNull Long shipmentItemId, @NotNull @Positive Integer quantity,
                                @NotBlank @Size(max = 255) String reason) {}
    public record TransferResponse(Long id, InventoryResponse source, InventoryResponse destination) {}
    public record ReturnResponse(Long id, Long shipmentItemId, InventoryResponse inventory) {}
    public record InventoryResponse(Long id, Long productId, Long warehouseId, int quantity, int available) {}
    public record MovementResponse(Long id, String type, int quantityChange, String reason) {}

    public record OrderLineRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {}
    public record DeliveryAddress(@NotBlank @Size(max = 255) String recipient,
                                  @NotBlank @Size(max = 255) String street,
                                  @NotBlank @Size(max = 100) String city,
                                  @NotBlank @Size(max = 30) String postalCode,
                                  @NotBlank @Size(max = 100) String country) {}
    public record OrderRequest(@NotNull Long customerId, @NotNull @Valid DeliveryAddress deliveryAddress,
                               @NotEmpty List<@NotNull @Valid OrderLineRequest> items) {}
    public record OrderLineResponse(Long id, Long productId, String productName,
                                    int quantity, BigDecimal price, String currency) {}
    public record OrderResponse(Long id, Long companyId, Long customerId, String customerName,
                                DeliveryAddress deliveryAddress, String status, List<OrderLineResponse> items) {}

    public record AllocationRequest(@NotNull Long orderItemId, @NotNull Long inventoryId,
                                    @NotNull @Positive Integer quantity) {}
    public record ConfirmRequest(@NotEmpty List<@NotNull @Valid AllocationRequest> allocations) {}

    public record ShipmentLineRequest(@NotNull Long orderItemId, @NotNull @Positive Integer quantity) {}
    public record ShipmentLineResponse(Long id, Long orderItemId, int quantity, long returnedQuantity) {}
    public record ShipmentRequest(@NotNull Long warehouseId, @Size(max = 100) String carrier,
                                  @Size(max = 100) String trackingNumber,
                                  @NotEmpty List<@NotNull @Valid ShipmentLineRequest> items) {}
    public record ShipmentResponse(Long id, Long orderId, Long warehouseId, String status,
                                   String carrier, String trackingNumber, List<ShipmentLineResponse> items) {}
}
