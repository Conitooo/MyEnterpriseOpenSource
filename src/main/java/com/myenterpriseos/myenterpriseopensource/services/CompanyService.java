package com.myenterpriseos.myenterpriseopensource.services;

import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.exception.CompanyAlreadyActiveException;
import com.myenterpriseos.myenterpriseopensource.exception.CompanyNotFoundException;
import com.myenterpriseos.myenterpriseopensource.mapper.CompanyMapper;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import com.myenterpriseos.myenterpriseopensource.dto.CompanyResponse;
import com.myenterpriseos.myenterpriseopensource.dto.CreateCompanyRequest;
import com.myenterpriseos.myenterpriseopensource.dto.UpdateCompanyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public CompanyResponse createCompany(CreateCompanyRequest request) {

        Company company = CompanyMapper.toEntity(request);

        return CompanyMapper.toResponse(
                companyRepository.save(company)
        );
    }

    @Transactional
    public void deleteCompany(Long id) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        if (company.getDeletedAt() == null) {
            company.setDeletedAt(LocalDateTime.now());
        }
    }

    @Transactional
    public void reactivateCompany(Long id) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        if (company.getDeletedAt() == null) {
            throw new CompanyAlreadyActiveException(id);
        }

        company.setDeletedAt(null);
    }

    @Transactional(readOnly = true)
    public CompanyResponse findCompanyById(Long id) {

        Company company = companyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        return CompanyMapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> findAllCompanies() {

        return companyRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(CompanyMapper::toResponse)
                .toList();
    }

    @Transactional
    public CompanyResponse updateCompany(Long id, UpdateCompanyRequest request) {

        Company company = companyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        company.setName(request.name());

        return CompanyMapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> findDeletedCompanies() {

        return companyRepository.findDeletedCompanies()
                .stream()
                .map(CompanyMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse findDeletedCompanyById(Long id) {

        Company company = companyRepository.findDeletedCompanyById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        return CompanyMapper.toResponse(company);
    }
}