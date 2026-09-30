package com.myenterpriseos.myenterpriseopensource;

import com.myenterpriseos.myenterpriseopensource.api.ApiException;
import com.myenterpriseos.myenterpriseopensource.dto.ApiDtos.*;
import com.myenterpriseos.myenterpriseopensource.entity.*;
import com.myenterpriseos.myenterpriseopensource.enums.*;
import com.myenterpriseos.myenterpriseopensource.repository.*;
import com.myenterpriseos.myenterpriseopensource.service.BusinessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessServiceUnitTests {
    @Mock CompanyRepository companies;
    @Mock ProductRepository products;
    @Mock WarehouseRepository warehouses;
    @Mock InventoryRepository inventories;
    @Mock InventoryMovementRepository movements;
    @Mock SalesOrderRepository orders;
    @Mock OrderItemRepository orderItems;
    @Mock StockReservationRepository reservations;
    @Mock ShipmentRepository shipments;
    @Mock ShipmentItemRepository shipmentItems;
    @InjectMocks BusinessService service;

    private Company company(long id) {
        Company owner = new Company();
        owner.setId(id);
        return owner;
    }

    @Test
    void duplicateSkuIsRejectedBeforeSaving() {
        when(companies.findById(1L)).thenReturn(Optional.of(company(1)));
        when(products.existsByCompanyIdAndSku(1L, "SKU")).thenReturn(true);
        assertThrows(ApiException.class, () -> service.createProduct(1L,
                new ProductRequest("Widget", "SKU", BigDecimal.ONE, "EUR")));
        verify(products, never()).save(any());
    }

    @Test
    void stockCannotCrossCompanyBoundary() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setCompany(company(1));
        Product product = new Product();
        product.setId(20L);
        product.setCompany(company(2));
        when(warehouses.findById(10L)).thenReturn(Optional.of(warehouse));
        when(products.findById(20L)).thenReturn(Optional.of(product));
        assertThrows(ApiException.class, () -> service.addStock(10L, new StockRequest(20L, 5)));
        verify(inventories, never()).save(any());
    }

    @Test
    void adjustmentCannotConsumeReservedQuantity() {
        Inventory stock = new Inventory();
        stock.setId(1L);
        stock.setQuantity(5);
        when(inventories.lockById(1L)).thenReturn(Optional.of(stock));
        when(reservations.sumQuantityByInventoryIdAndStatus(1L, ReservationStatus.ACTIVE))
                .thenReturn(4L);
        assertThrows(ApiException.class, () -> service.adjust(1L, new AdjustmentRequest(-2, "damage")));
        assertEquals(5, stock.getQuantity());
        verify(movements, never()).save(any());
    }

    @Test
    void confirmedOrderCannotBeConfirmedAgain() {
        SalesOrder order = new SalesOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        when(orders.lockById(1L)).thenReturn(Optional.of(order));
        assertThrows(ApiException.class, () -> service.confirm(1L,
                new ConfirmRequest(List.of(new AllocationRequest(1L, 1L, 1)))));
        verifyNoInteractions(reservations);
    }
}
