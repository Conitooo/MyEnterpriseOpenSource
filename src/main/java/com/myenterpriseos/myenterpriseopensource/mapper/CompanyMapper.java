package com.myenterpriseos.myenterpriseopensource.mapper;

import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.dto.CompanyResponse;
import com.myenterpriseos.myenterpriseopensource.dto.CreateCompanyRequest;

public class CompanyMapper {

    public static Company toEntity(CreateCompanyRequest request) {
        Company company = new Company();
        company.setName(request.name());
        return company;
    }

    public static CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName()
        );
    }

}
