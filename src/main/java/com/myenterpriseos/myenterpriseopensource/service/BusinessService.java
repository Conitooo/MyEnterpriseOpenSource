package com.myenterpriseos.myenterpriseopensource.service;

import com.myenterpriseos.myenterpriseopensource.api.ApiException;
import com.myenterpriseos.myenterpriseopensource.dto.ApiDtos.*;
import com.myenterpriseos.myenterpriseopensource.entity.*;
import com.myenterpriseos.myenterpriseopensource.enums.*;
import com.myenterpriseos.myenterpriseopensource.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final CustomerRepository customers;
    private final StockTransferRepository transfers;
    private final StockReturnRepository returns;

    public BusinessService(CompanyRepository companies, ProductRepository products, WarehouseRepository warehouses,
                           InventoryRepository inventories, InventoryMovementRepository movements,
                           SalesOrderRepository orders, OrderItemRepository orderItems,
                           StockReservationRepository reservations, ShipmentRepository shipments,
                           ShipmentItemRepository shipmentItems, CustomerRepository customers,
                           StockTransferRepository transfers, StockReturnRepository returns) {
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
        this.customers = customers;
        this.transfers = transfers;
        this.returns = returns;
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

    private static String trimmed(String value) { return value == null ? null : value.trim(); }

    public CustomerResponse createCustomer(Long companyId, CustomerRequest body) {
        Customer item = new Customer();
        item.setCompany(company(companyId));
        updateCustomerFields(item, body);
        customers.save(item);
        return customerResponse(item);
    }

    public CustomerResponse updateCustomer(Long companyId, Long customerId, CustomerRequest body) {
        Customer item = customer(customerId, companyId);
        updateCustomerFields(item, body);
        return customerResponse(item);
    }

    public CustomerResponse deactivateCustomer(Long companyId, Long customerId) {
        Customer item = customer(customerId, companyId);
        item.setActive(false);
        return customerResponse(item);
    }

    public PageResponse<CustomerResponse> customers(Long companyId, String query, int page, int size) {
        company(companyId);
        Page<Customer> result = customers.findByCompanyIdAndNameContainingIgnoreCase(companyId,
                query == null ? "" : query.trim(), pageRequest(page, size));
        return pageResponse(result.map(this::customerResponse));
    }

    private Customer customer(Long id, Long companyId) {
        Customer item = customers.findById(id).orElseThrow(() -> missing("Customer"));
        if (!same(item.getCompany().getId(), companyId)) throw missing("Customer");
        return item;
    }

    private void updateCustomerFields(Customer item, CustomerRequest body) {
        item.setName(body.name().trim());
        item.setEmail(trimmed(body.email()));
        item.setPhone(trimmed(body.phone()));
    }

    private CustomerResponse customerResponse(Customer item) {
        return new CustomerResponse(item.getId(), item.getCompany().getId(), item.getName(),
                item.getEmail(), item.getPhone(), item.isActive());
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw bad("Invalid page or size");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }

    private <T> PageResponse<T> pageResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(),
                page.getNumber(), page.getSize());
    }

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

    public PageResponse<ProductResponse> productPage(Long companyId, String query, int page, int size) {
        company(companyId);
        return pageResponse(products.findByCompanyIdAndProductNameContainingIgnoreCase(companyId,
                query == null ? "" : query.trim(), pageRequest(page, size)).map(this::productResponse));
    }

    public ProductResponse updateProduct(Long companyId, Long productId, ProductRequest body) {
        Product item = product(productId);
        if (!same(item.getCompany().getId(), companyId)) throw missing("Product");
        String sku = body.sku().trim();
        if (!item.getSku().equals(sku) && products.existsByCompanyIdAndSku(companyId, sku))
            throw conflict("SKU already exists in company");
        item.setProductName(body.productName().trim());
        item.setSku(sku);
        item.setPrice(body.price());
        item.setCurrency(body.currency());
        return productResponse(item);
    }

    public ProductResponse deactivateProduct(Long companyId, Long productId) {
        Product item = product(productId);
        if (!same(item.getCompany().getId(), companyId)) throw missing("Product");
        item.setActive(false);
        return productResponse(item);
    }

    private ProductResponse productResponse(Product item) {
        return new ProductResponse(item.getId(), item.getCompany().getId(), item.getProductName(),
                item.getSku(), item.getPrice(), item.getCurrency(), item.isActive());
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

    public WarehouseResponse updateWarehouse(Long companyId, Long warehouseId, WarehouseRequest body) {
        Warehouse item = warehouse(warehouseId);
        if (!same(item.getCompany().getId(), companyId)) throw missing("Warehouse");
        String code = body.code().trim();
        if (!item.getCode().equals(code) && warehouses.existsByCompanyIdAndCode(companyId, code))
            throw conflict("Warehouse code already exists in company");
        item.setCode(code);
        item.setName(body.name().trim());
        return warehouseResponse(item);
    }

    public WarehouseResponse deactivateWarehouse(Long companyId, Long warehouseId) {
        Warehouse item = warehouses.lockById(warehouseId).orElseThrow(() -> missing("Warehouse"));
        if (!same(item.getCompany().getId(), companyId)) throw missing("Warehouse");
        if (inventories.findByWarehouseId(warehouseId).stream().anyMatch(i -> i.getQuantity() > 0))
            throw conflict("Move or remove stock before deactivating warehouse");
        item.setActive(false);
        return warehouseResponse(item);
    }

    private WarehouseResponse warehouseResponse(Warehouse item) {
        return new WarehouseResponse(item.getId(), item.getCompany().getId(), item.getCode(), item.getName(), item.isActive());
    }

    public InventoryResponse addStock(Long warehouseId, StockRequest body) {
        Warehouse location = warehouses.lockById(warehouseId).orElseThrow(() -> missing("Warehouse"));
        Product item = product(body.productId());
        if (!location.isActive() || !item.isActive()) throw conflict("Product and warehouse must be active");
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
        List<Inventory> stocks = inventories.findByWarehouseId(warehouseId);
        if (stocks.isEmpty()) return List.of();
        Map<Long, Long> reserved = new HashMap<>();
        for (Object[] row : reservations.totalsByInventoryIds(
                stocks.stream().map(Inventory::getId).toList(), ReservationStatus.ACTIVE)) {
            reserved.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return stocks.stream().map(stock -> inventoryResponse(stock, reserved.getOrDefault(stock.getId(), 0L))).toList();
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

    public InventoryResponse stocktake(Long inventoryId, StocktakeRequest body) {
        Inventory stock = inventories.lockById(inventoryId).orElseThrow(() -> missing("Inventory"));
        int change = body.countedQuantity() - stock.getQuantity();
        if (change == 0) return inventoryResponse(stock);
        if (body.countedQuantity() < activeReserved(inventoryId))
            throw conflict("Counted stock is below active reservations");
        stock.setQuantity(body.countedQuantity());
        recordMovement(stock, change > 0 ? MovementType.ADJUSTMENT_IN : MovementType.ADJUSTMENT_OUT,
                change, "Stocktake: " + body.reason().trim());
        return inventoryResponse(stock);
    }

    public TransferResponse transfer(Long companyId, TransferRequest body) {
        if (same(body.sourceWarehouseId(), body.destinationWarehouseId()))
            throw bad("Source and destination must differ");
        Map<Long, Warehouse> locations = new HashMap<>();
        for (Long id : java.util.stream.Stream.of(body.sourceWarehouseId(), body.destinationWarehouseId())
                .sorted().toList()) {
            locations.put(id, warehouses.lockById(id).orElseThrow(() -> missing("Warehouse")));
        }
        Warehouse source = locations.get(body.sourceWarehouseId());
        Warehouse destination = locations.get(body.destinationWarehouseId());
        Product item = product(body.productId());
        if (!same(source.getCompany().getId(), companyId) ||
                !same(destination.getCompany().getId(), companyId) ||
                !same(item.getCompany().getId(), companyId)) throw bad("Transfer must stay within one company");
        if (!source.isActive() || !destination.isActive() || !item.isActive())
            throw conflict("Transfer locations and product must be active");
        Inventory from = inventories.findByProductIdAndWarehouseId(item.getId(), source.getId())
                .orElseThrow(() -> missing("Inventory"));
        Inventory to = inventories.findByProductIdAndWarehouseId(item.getId(), destination.getId())
                .orElse(null);
        for (Long id : java.util.stream.Stream.of(from.getId(), to == null ? null : to.getId())
                .filter(Objects::nonNull).sorted().toList()) {
            Inventory locked = inventories.lockById(id).orElseThrow(() -> missing("Inventory"));
            if (id.equals(from.getId())) from = locked;
            else to = locked;
        }
        if (from.getQuantity() - activeReserved(from.getId()) < body.quantity())
            throw conflict("Insufficient available stock to transfer");
        if (to == null) {
            to = new Inventory();
            to.setProduct(item);
            to.setWarehouse(destination);
            to.setQuantity(0);
            inventories.saveAndFlush(to);
        }
        long next = (long) to.getQuantity() + body.quantity();
        if (next > Integer.MAX_VALUE) throw bad("Inventory quantity exceeds supported range");
        from.setQuantity(from.getQuantity() - body.quantity());
        to.setQuantity((int) next);
        StockTransfer transfer = new StockTransfer();
        transfer.setCompany(source.getCompany());
        transfer.setProduct(item);
        transfer.setSourceWarehouse(source);
        transfer.setDestinationWarehouse(destination);
        transfer.setQuantity(body.quantity());
        transfers.saveAndFlush(transfer);
        recordMovement(from, MovementType.TRANSFER_OUT, -body.quantity(), "Transfer #" + transfer.getId());
        recordMovement(to, MovementType.TRANSFER_IN, body.quantity(), "Transfer #" + transfer.getId());
        return new TransferResponse(transfer.getId(), inventoryResponse(from), inventoryResponse(to));
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
        // A locking read sees reservations committed while this transaction waited for the inventory lock.
        // A regular aggregate can use an older REPEATABLE_READ snapshot and allow overselling.
        return reservations.activeQuantitiesForUpdate(inventoryId).stream().mapToLong(Number::longValue).sum();
    }

    private InventoryResponse inventoryResponse(Inventory stock) {
        return inventoryResponse(stock, activeReserved(stock.getId()));
    }

    private InventoryResponse inventoryResponse(Inventory stock, long reserved) {
        return new InventoryResponse(stock.getId(), stock.getProduct().getId(), stock.getWarehouse().getId(),
                stock.getQuantity(), (int) (stock.getQuantity() - reserved));
    }

    public OrderResponse createOrder(Long companyId, OrderRequest body) {
        Company owner = company(companyId);
        Customer buyer = customer(body.customerId(), companyId);
        if (!buyer.isActive()) throw conflict("Customer is inactive");
        Set<Long> seen = new HashSet<>();
        for (OrderLineRequest line : body.items()) {
            if (!seen.add(line.productId())) throw bad("Duplicate product in order");
        }
        Map<Long, Product> orderProducts = products.findAllById(seen).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        for (Long productId : seen) {
            Product item = orderProducts.get(productId);
            if (item == null) throw missing("Product");
            if (!same(item.getCompany().getId(), companyId)) throw bad("Product belongs to another company");
            if (!item.isActive()) throw conflict("Product is inactive");
        }
        SalesOrder order = new SalesOrder();
        order.setCompany(owner);
        order.setCustomer(buyer);
        DeliveryAddress address = body.deliveryAddress();
        order.setRecipient(address.recipient().trim());
        order.setDeliveryStreet(address.street().trim());
        order.setDeliveryCity(address.city().trim());
        order.setDeliveryPostalCode(address.postalCode().trim());
        order.setDeliveryCountry(address.country().trim());
        order.setStatus(OrderStatus.DRAFT);
        orders.save(order);
        for (OrderLineRequest line : body.items()) {
            Product product = orderProducts.get(line.productId());
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

    public List<OrderResponse> orders(Long companyId) {
        company(companyId);
        List<SalesOrder> companyOrders = orders.findByCompanyId(companyId);
        if (companyOrders.isEmpty()) return List.of();
        Map<Long, List<OrderLineResponse>> lines = orderItems.findByCompanyId(companyId).stream()
                .collect(Collectors.groupingBy(i -> i.getOrder().getId(), Collectors.mapping(this::orderLineResponse,
                        Collectors.toList())));
        return companyOrders.stream().map(order -> orderResponse(order,
                lines.getOrDefault(order.getId(), List.of()))).toList();
    }

    public PageResponse<OrderResponse> orderPage(Long companyId, int page, int size) {
        company(companyId);
        Page<SalesOrder> result = orders.findByCompanyId(companyId, pageRequest(page, size));
        List<Long> ids = result.getContent().stream().map(SalesOrder::getId).toList();
        Map<Long, List<OrderLineResponse>> lines = ids.isEmpty() ? Map.of() :
                orderItems.findByOrderIdIn(ids).stream().collect(Collectors.groupingBy(i -> i.getOrder().getId(),
                        Collectors.mapping(this::orderLineResponse, Collectors.toList())));
        return new PageResponse<>(result.getContent().stream().map(o ->
                orderResponse(o, lines.getOrDefault(o.getId(), List.of()))).toList(),
                result.getTotalElements(), page, size);
    }

    public List<ShipmentResponse> shipments(Long orderId) {
        orderEntity(orderId);
        return shipments.findByOrderId(orderId).stream()
                .map(this::shipmentResponse).toList();
    }

    private ShipmentResponse shipmentResponse(Shipment shipment) {
        return new ShipmentResponse(shipment.getId(), shipment.getOrder().getId(),
                shipment.getWarehouse().getId(), shipment.getStatus().name(), shipment.getCarrier(),
                shipment.getTrackingNumber(), shipmentItems.findByShipmentId(shipment.getId()).stream()
                        .map(i -> new ShipmentLineResponse(i.getId(), i.getOrderItem().getId(),
                                i.getQuantity(), returns.returnedQuantity(i.getId()))).toList());
    }

    private OrderResponse orderResponse(SalesOrder order) {
        List<OrderLineResponse> lines = orderItems.findByOrderId(order.getId()).stream()
                .map(this::orderLineResponse).toList();
        return orderResponse(order, lines);
    }

    private OrderResponse orderResponse(SalesOrder order, List<OrderLineResponse> lines) {
        Customer buyer = order.getCustomer();
        DeliveryAddress address = order.getRecipient() == null ? null : new DeliveryAddress(
                order.getRecipient(), order.getDeliveryStreet(), order.getDeliveryCity(),
                order.getDeliveryPostalCode(), order.getDeliveryCountry());
        return new OrderResponse(order.getId(), order.getCompany().getId(),
                buyer == null ? null : buyer.getId(), buyer == null ? null : buyer.getName(),
                address, order.getStatus().name(), lines);
    }

    private OrderLineResponse orderLineResponse(OrderItem item) {
        return new OrderLineResponse(item.getId(), item.getProduct().getId(), item.getProduct().getProductName(), item.getQuantity(),
                item.getPrice(), item.getCurrency());
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
        if (shipments.existsByOrderId(orderId)) throw conflict("An order with shipments cannot be cancelled");
        for (StockReservation reservation : reservations.findByOrderId(orderId)) {
            if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                reservation.setStatus(ReservationStatus.RELEASED);
                reservation.setReleasedAt(Instant.now());
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
        Map<Long, Long> shippedTotals = new HashMap<>();
        for (Object[] row : shipmentItems.shippedTotalsByOrderId(orderId)) {
            shippedTotals.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        Map<Long, Integer> requested = new HashMap<>();
        for (ShipmentLineRequest line : body.items()) {
            if (!lines.containsKey(line.orderItemId())) throw bad("Order item does not belong to order");
            if (requested.putIfAbsent(line.orderItemId(), line.quantity()) != null) throw bad("Duplicate shipment item");
        }
        Map<Long, List<StockReservation>> reservationsByLine = reservations.findByOrderId(orderId).stream()
                .collect(Collectors.groupingBy(r -> r.getOrderItem().getId()));
        Map<Long, StockReservation> selected = new HashMap<>();
        for (var entry : requested.entrySet()) {
            OrderItem line = lines.get(entry.getKey());
            long alreadyShipped = shippedTotals.getOrDefault(line.getId(), 0L);
            if (alreadyShipped + entry.getValue() > line.getQuantity()) throw conflict("Shipment exceeds order quantity");
            StockReservation reservation = reservationsByLine.getOrDefault(line.getId(), List.of()).stream()
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
        shipment.setCarrier(trimmed(body.carrier()));
        shipment.setTrackingNumber(trimmed(body.trackingNumber()));
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
            shippedTotals.merge(entry.getKey(), (long) quantity, Long::sum);
            recordMovement(stock, MovementType.SHIPMENT, -quantity, "Shipment " + shipment.getId());
        }
        boolean complete = lines.values().stream().allMatch(line ->
                shippedTotals.getOrDefault(line.getId(), 0L) == line.getQuantity().longValue());
        if (complete) order.setStatus(OrderStatus.SHIPPED);
        return shipmentResponse(shipment);
    }

    public ReturnResponse returnStock(Long orderId, ReturnRequest body) {
        SalesOrder order = lockedOrder(orderId);
        ShipmentItem shipped = shipmentItems.lockById(body.shipmentItemId())
                .orElseThrow(() -> missing("Shipment item"));
        if (!same(shipped.getShipment().getOrder().getId(), order.getId()))
            throw missing("Shipment item");
        long previouslyReturned = returns.returnedQuantity(shipped.getId());
        if (previouslyReturned + body.quantity() > shipped.getQuantity())
            throw conflict("Return exceeds shipped quantity");
        Warehouse location = shipped.getShipment().getWarehouse();
        Product item = shipped.getOrderItem().getProduct();
        Inventory stock = inventories.findByProductIdAndWarehouseId(item.getId(), location.getId())
                .orElseThrow(() -> missing("Inventory"));
        stock = inventories.lockById(stock.getId()).orElseThrow(() -> missing("Inventory"));
        long next = (long) stock.getQuantity() + body.quantity();
        if (next > Integer.MAX_VALUE) throw bad("Inventory quantity exceeds supported range");
        stock.setQuantity((int) next);
        StockReturn returned = new StockReturn();
        returned.setShipmentItem(shipped);
        returned.setQuantity(body.quantity());
        returned.setReason(body.reason().trim());
        returns.saveAndFlush(returned);
        recordMovement(stock, MovementType.ADJUSTMENT_IN, body.quantity(), "Return #" + returned.getId() + ": " + body.reason().trim());
        return new ReturnResponse(returned.getId(), shipped.getId(), inventoryResponse(stock));
    }
}
