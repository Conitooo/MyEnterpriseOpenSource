package com.myenterpriseos.myenterpriseopensource.service;

import com.myenterpriseos.myenterpriseopensource.api.ApiException;
import com.myenterpriseos.myenterpriseopensource.dto.ApiDtos.*;
import com.myenterpriseos.myenterpriseopensource.entity.*;
import com.myenterpriseos.myenterpriseopensource.enums.*;
import com.myenterpriseos.myenterpriseopensource.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BusinessService {
    private final CompanyRepository companies;
    private final ProductRepository products;
    private final WarehouseRepository warehouses;
    private final InventoryRepository inventories;
    private final InventoryMovementRepository movements;
    private final SalesOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final StockReservationRepository reservations;
    private final ShipmentRepository shipments;
    private final ShipmentItemRepository shipmentItems;

    public BusinessService(CompanyRepository companies, ProductRepository products, WarehouseRepository warehouses,
                           InventoryRepository inventories, InventoryMovementRepository movements,
                           SalesOrderRepository orders, OrderItemRepository orderItems,
                           StockReservationRepository reservations, ShipmentRepository shipments,
                           ShipmentItemRepository shipmentItems) {
        this.companies = companies;
        this.products = products;
        this.warehouses = warehouses;
        this.inventories = inventories;
        this.movements = movements;
        this.orders = orders;
        this.orderItems = orderItems;
        this.reservations = reservations;
        this.shipments = shipments;
        this.shipmentItems = shipmentItems;
    }

    private ApiException bad(String message) { return new ApiException(HttpStatus.BAD_REQUEST, message); }
    private ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
    private ApiException missing(String type) { return new ApiException(HttpStatus.NOT_FOUND, type + " not found"); }
    private Company company(Long id) { return companies.findById(id).orElseThrow(() -> missing("Company")); }
    private Product product(Long id) { return products.findById(id).orElseThrow(() -> missing("Product")); }
    private Warehouse warehouse(Long id) { return warehouses.findById(id).orElseThrow(() -> missing("Warehouse")); }
    private Inventory findInventory(Long id) { return inventories.findById(id).orElseThrow(() -> missing("Inventory")); }
    private SalesOrder orderEntity(Long id) { return orders.findById(id).orElseThrow(() -> missing("Order")); }
    private SalesOrder lockedOrder(Long id) { return orders.lockById(id).orElseThrow(() -> missing("Order")); }
    private static boolean same(Long left, Long right) { return Objects.equals(left, right); }

    public CompanyResponse createCompany(CompanyRequest body) {
        Company item = new Company();
        item.setName(body.name().trim());
        companies.save(item);
        return new CompanyResponse(item.getId(), item.getName());
    }

    public ProductResponse createProduct(Long companyId, ProductRequest body) {
        Company owner = company(companyId);
        String sku = body.sku().trim();
        if (products.existsByCompanyIdAndSku(companyId, sku)) throw conflict("SKU already exists in company");
        Product item = new Product();
        item.setCompany(owner);
        item.setProductName(body.productName().trim());
        item.setSku(sku);
        item.setPrice(body.price());
        item.setCurrency(body.currency());
        products.save(item);
        return productResponse(item);
    }

    public List<ProductResponse> products(Long companyId) {
        company(companyId);
        return products.findByCompanyId(companyId).stream().map(this::productResponse).toList();
    }

    private ProductResponse productResponse(Product item) {
        return new ProductResponse(item.getId(), item.getCompany().getId(), item.getProductName(),
                item.getSku(), item.getPrice(), item.getCurrency());
    }

    public WarehouseResponse createWarehouse(Long companyId, WarehouseRequest body) {
        Company owner = company(companyId);
        String code = body.code().trim();
        if (warehouses.existsByCompanyIdAndCode(companyId, code)) throw conflict("Warehouse code already exists in company");
        Warehouse item = new Warehouse();
        item.setCompany(owner);
        item.setCode(code);
        item.setName(body.name().trim());
        warehouses.save(item);
        return warehouseResponse(item);
    }

    public List<WarehouseResponse> warehouses(Long companyId) {
        company(companyId);
        return warehouses.findByCompanyId(companyId).stream().map(this::warehouseResponse).toList();
    }

    private WarehouseResponse warehouseResponse(Warehouse item) {
        return new WarehouseResponse(item.getId(), item.getCompany().getId(), item.getCode(), item.getName());
    }

    public InventoryResponse addStock(Long warehouseId, StockRequest body) {
        Warehouse location = warehouse(warehouseId);
        Product item = product(body.productId());
        if (!same(location.getCompany().getId(), item.getCompany().getId())) throw bad("Product and warehouse belong to different companies");
        Inventory stock = inventories.findByProductIdAndWarehouseId(item.getId(), location.getId()).orElse(null);
        if (stock == null) {
            stock = new Inventory();
            stock.setProduct(item);
            stock.setWarehouse(location);
            stock.setQuantity(body.quantity());
            inventories.saveAndFlush(stock);
            recordMovement(stock, MovementType.INITIAL_STOCK, body.quantity(), "Initial stock");
        } else {
            stock = inventories.lockById(stock.getId()).orElseThrow(() -> missing("Inventory"));
            long next = (long) stock.getQuantity() + body.quantity();
            if (next > Integer.MAX_VALUE) throw bad("Inventory quantity exceeds supported range");
            stock.setQuantity((int) next);
            recordMovement(stock, MovementType.ADJUSTMENT_IN, body.quantity(), "Stock receipt");
        }
        return inventoryResponse(stock);
    }

    public List<InventoryResponse> inventory(Long warehouseId) {
        warehouse(warehouseId);
        return inventories.findByWarehouseId(warehouseId).stream().map(this::inventoryResponse).toList();
    }

    public InventoryResponse adjust(Long inventoryId, AdjustmentRequest body) {
        Inventory stock = inventories.lockById(inventoryId).orElseThrow(() -> missing("Inventory"));
        int delta = body.quantityChange();
        if (delta == 0) throw bad("Quantity change must not be zero");
        long next = (long) stock.getQuantity() + delta;
        if (next < activeReserved(stock.getId())) throw conflict("Adjustment would consume reserved stock");
        if (next > Integer.MAX_VALUE) throw bad("Inventory quantity exceeds supported range");
        stock.setQuantity((int) next);
        recordMovement(stock, delta > 0 ? MovementType.ADJUSTMENT_IN : MovementType.ADJUSTMENT_OUT,
                delta, body.reason().trim());
        return inventoryResponse(stock);
    }

    public List<MovementResponse> movements(Long inventoryId) {
        findInventory(inventoryId);
        return movements.findByInventoryIdOrderByIdDesc(inventoryId).stream()
                .map(m -> new MovementResponse(m.getId(), m.getMovementType().name(),
                        m.getQuantityChange(), m.getReason())).toList();
    }

    private void recordMovement(Inventory stock, MovementType type, int change, String reason) {
        InventoryMovement movement = new InventoryMovement();
        movement.setInventory(stock);
        movement.setMovementType(type);
        movement.setQuantityChange(change);
        movement.setReason(reason);
        movements.save(movement);
    }

    private long activeReserved(Long inventoryId) {
        return reservations.findByInventoryIdAndStatus(inventoryId, ReservationStatus.ACTIVE)
                .stream().mapToLong(StockReservation::getQuantity).sum();
    }

    private InventoryResponse inventoryResponse(Inventory stock) {
        long reserved = activeReserved(stock.getId());
        return new InventoryResponse(stock.getId(), stock.getProduct().getId(), stock.getWarehouse().getId(),
                stock.getQuantity(), (int) (stock.getQuantity() - reserved));
    }

    public OrderResponse createOrder(Long companyId, OrderRequest body) {
        Company owner = company(companyId);
        Set<Long> seen = new HashSet<>();
        for (OrderLineRequest line : body.items()) {
            if (!seen.add(line.productId())) throw bad("Duplicate product in order");
        }
        SalesOrder order = new SalesOrder();
        order.setCompany(owner);
        order.setStatus(OrderStatus.DRAFT);
        orders.save(order);
        for (OrderLineRequest line : body.items()) {
            Product product = product(line.productId());
            if (!same(product.getCompany().getId(), companyId)) throw bad("Product belongs to another company");
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setPrice(product.getPrice());
            item.setCurrency(product.getCurrency());
            orderItems.save(item);
        }
        return orderResponse(order);
    }

    public OrderResponse order(Long orderId) { return orderResponse(orderEntity(orderId)); }

    private OrderResponse orderResponse(SalesOrder order) {
        List<OrderLineResponse> lines = orderItems.findByOrderId(order.getId()).stream()
                .map(i -> new OrderLineResponse(i.getId(), i.getProduct().getId(), i.getQuantity(),
                        i.getPrice(), i.getCurrency())).toList();
        return new OrderResponse(order.getId(), order.getCompany().getId(), order.getStatus().name(), lines);
    }

    public OrderResponse confirm(Long orderId, ConfirmRequest body) {
        SalesOrder order = lockedOrder(orderId);
        if (order.getStatus() != OrderStatus.DRAFT) throw conflict("Only draft orders can be confirmed");
        List<OrderItem> lines = orderItems.findByOrderId(orderId);
        Map<Long, OrderItem> byId = lines.stream().collect(Collectors.toMap(OrderItem::getId, i -> i));
        Map<Long, Long> allocatedByLine = new HashMap<>();
        Map<Long, Long> allocatedByStock = new HashMap<>();
        Set<String> pairs = new HashSet<>();
        for (AllocationRequest allocation : body.allocations()) {
            if (!byId.containsKey(allocation.orderItemId())) throw bad("Order item does not belong to order");
            if (!pairs.add(allocation.orderItemId() + ":" + allocation.inventoryId())) throw bad("Duplicate allocation");
            allocatedByLine.merge(allocation.orderItemId(), allocation.quantity().longValue(), Long::sum);
            allocatedByStock.merge(allocation.inventoryId(), allocation.quantity().longValue(), Long::sum);
        }
        for (OrderItem line : lines) {
            if (allocatedByLine.getOrDefault(line.getId(), 0L) != line.getQuantity().longValue())
                throw bad("Every order item must be fully allocated");
        }
        Map<Long, Inventory> locked = new HashMap<>();
        for (Long stockId : allocatedByStock.keySet().stream().sorted().toList()) {
            Inventory stock = inventories.lockById(stockId).orElseThrow(() -> missing("Inventory"));
            if (stock.getQuantity() - activeReserved(stockId) < allocatedByStock.get(stockId))
                throw conflict("Insufficient available stock");
            locked.put(stockId, stock);
        }
        for (AllocationRequest allocation : body.allocations()) {
            OrderItem line = byId.get(allocation.orderItemId());
            Inventory stock = locked.get(allocation.inventoryId());
            if (!same(stock.getProduct().getId(), line.getProduct().getId()) ||
                    !same(stock.getWarehouse().getCompany().getId(), order.getCompany().getId()))
                throw bad("Allocation product or company does not match order");
            StockReservation reservation = new StockReservation();
            reservation.setOrderItem(line);
            reservation.setInventory(stock);
            reservation.setQuantity(allocation.quantity());
            reservation.setStatus(ReservationStatus.ACTIVE);
            reservations.save(reservation);
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now());
        return orderResponse(order);
    }

    public OrderResponse cancel(Long orderId) {
        SalesOrder order = lockedOrder(orderId);
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.CONFIRMED)
            throw conflict("Order cannot be cancelled");
        if (!shipments.findByOrderId(orderId).isEmpty()) throw conflict("An order with shipments cannot be cancelled");
        for (OrderItem line : orderItems.findByOrderId(orderId)) {
            for (StockReservation reservation : reservations.findByOrderItemId(line.getId())) {
                if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                    reservation.setStatus(ReservationStatus.RELEASED);
                    reservation.setReleasedAt(Instant.now());
                }
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        return orderResponse(order);
    }

    public ShipmentResponse ship(Long orderId, ShipmentRequest body) {
        SalesOrder order = lockedOrder(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) throw conflict("Only confirmed orders can be shipped");
        Warehouse location = warehouse(body.warehouseId());
        if (!same(location.getCompany().getId(), order.getCompany().getId())) throw bad("Warehouse belongs to another company");
        Map<Long, OrderItem> lines = orderItems.findByOrderId(orderId).stream()
                .collect(Collectors.toMap(OrderItem::getId, i -> i));
        Map<Long, Integer> requested = new HashMap<>();
        for (ShipmentLineRequest line : body.items()) {
            if (!lines.containsKey(line.orderItemId())) throw bad("Order item does not belong to order");
            if (requested.putIfAbsent(line.orderItemId(), line.quantity()) != null) throw bad("Duplicate shipment item");
        }
        Map<Long, StockReservation> selected = new HashMap<>();
        for (var entry : requested.entrySet()) {
            OrderItem line = lines.get(entry.getKey());
            long alreadyShipped = shipmentItems.findByOrderItemId(line.getId()).stream()
                    .mapToLong(ShipmentItem::getQuantity).sum();
            if (alreadyShipped + entry.getValue() > line.getQuantity()) throw conflict("Shipment exceeds order quantity");
            StockReservation reservation = reservations.findByOrderItemId(line.getId()).stream()
                    .filter(r -> r.getStatus() == ReservationStatus.ACTIVE &&
                            same(r.getInventory().getWarehouse().getId(), location.getId()))
                    .findFirst().orElseThrow(() -> conflict("No active reservation in warehouse"));
            if (reservation.getQuantity() < entry.getValue()) throw conflict("Shipment exceeds reserved quantity");
            selected.put(line.getId(), reservation);
        }
        Map<Long, Inventory> locked = new HashMap<>();
        for (Long stockId : selected.values().stream().map(r -> r.getInventory().getId()).distinct().sorted().toList()) {
            locked.put(stockId, inventories.lockById(stockId).orElseThrow(() -> missing("Inventory")));
        }
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setWarehouse(location);
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setShippedAt(Instant.now());
        shipments.save(shipment);
        for (var entry : requested.entrySet()) {
            StockReservation reservation = selected.get(entry.getKey());
            Inventory stock = locked.get(reservation.getInventory().getId());
            int quantity = entry.getValue();
            if (stock.getQuantity() < quantity) throw conflict("Insufficient stock to ship");
            stock.setQuantity(stock.getQuantity() - quantity);
            if (reservation.getQuantity() == quantity) {
                reservation.setStatus(ReservationStatus.CONSUMED);
            } else {
                reservation.setQuantity(reservation.getQuantity() - quantity);
            }
            ShipmentItem shipped = new ShipmentItem();
            shipped.setShipment(shipment);
            shipped.setOrderItem(lines.get(entry.getKey()));
            shipped.setQuantity(quantity);
            shipmentItems.save(shipped);
            recordMovement(stock, MovementType.SHIPMENT, -quantity, "Shipment " + shipment.getId());
        }
        boolean complete = lines.values().stream().allMatch(line ->
                shipmentItems.findByOrderItemId(line.getId()).stream().mapToLong(ShipmentItem::getQuantity).sum()
                        == line.getQuantity());
        if (complete) order.setStatus(OrderStatus.SHIPPED);
        return new ShipmentResponse(shipment.getId(), orderId, location.getId(), shipment.getStatus().name());
    }
}
