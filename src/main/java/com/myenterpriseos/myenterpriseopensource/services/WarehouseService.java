package com.myenterpriseos.myenterpriseopensource.services;

import com.myenterpriseos.myenterpriseopensource.dto.WarehouseRequest;
import com.myenterpriseos.myenterpriseopensource.dto.WarehouseResponse;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.entity.Warehouse;
import com.myenterpriseos.myenterpriseopensource.exception.CompanyNotFoundException;
import com.myenterpriseos.myenterpriseopensource.mapper.WarehouseMapper;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import com.myenterpriseos.myenterpriseopensource.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final CompanyRepository companyRepository;

    public WarehouseService(WarehouseRepository warehouseRepository, CompanyRepository companyRepository) {
        this.warehouseRepository = warehouseRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public WarehouseResponse createWarehouse(Long companyId, WarehouseRequest request) {

        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));

        Warehouse warehouse = WarehouseMapper.toEntity(request, company);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return WarehouseMapper.toResponse(savedWarehouse);
    }

}
