package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company,Long> {

    Optional<Company> findByIdAndDeletedAtIsNull(Long id);

    List<Company> findAllByDeletedAtIsNull();

    List<Company> findDeletedCompanies();

    Optional<Company> findDeletedCompanyById(Long id);
}
