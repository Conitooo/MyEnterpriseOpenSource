package com.myenterpriseos.myenterpriseopensource.mapper;

import com.myenterpriseos.myenterpriseopensource.dto.WarehouseRequest;
import com.myenterpriseos.myenterpriseopensource.dto.WarehouseResponse;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.entity.Warehouse;

public class WarehouseMapper {

    public static Warehouse toEntity(WarehouseRequest request, Company company) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setCompany(company);
        return warehouse;
    }

    public static WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getCompany().getName()
        );
    }
}
