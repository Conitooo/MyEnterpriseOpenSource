package com.myenterpriseos.myenterpriseopensource.security;

import com.myenterpriseos.myenterpriseopensource.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Component("tenantGuard")
@Transactional(readOnly = true)
public class TenantGuard {
    private final WarehouseRepository warehouses;
    private final InventoryRepository inventories;
    private final SalesOrderRepository orders;

    public TenantGuard(WarehouseRepository warehouses, InventoryRepository inventories, SalesOrderRepository orders) {
        this.warehouses = warehouses;
        this.inventories = inventories;
        this.orders = orders;
    }

    public Long currentCompanyId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) ||
                !(token.getToken().getClaim("company_id") instanceof Number companyId)) return null;
        return companyId.longValue();
    }

    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) return null;
        try { return Long.valueOf(token.getToken().getSubject()); }
        catch (RuntimeException ex) { return null; }
    }

    public boolean company(Long companyId) { return companyId != null && Objects.equals(currentCompanyId(), companyId); }
    public boolean warehouse(Long warehouseId) {
        return warehouseId != null && warehouses.findById(warehouseId)
                .map(w -> company(w.getCompany().getId())).orElse(false);
    }
    public boolean inventory(Long inventoryId) {
        return inventoryId != null && inventories.findById(inventoryId)
                .map(i -> company(i.getWarehouse().getCompany().getId()) &&
                        company(i.getProduct().getCompany().getId())).orElse(false);
    }
    public boolean order(Long orderId) {
        return orderId != null && orders.findById(orderId)
                .map(o -> company(o.getCompany().getId())).orElse(false);
    }
}
