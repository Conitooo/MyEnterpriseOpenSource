package com.myenterpriseos.myenterpriseopensource.repository;

import com.myenterpriseos.myenterpriseopensource.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByCompanyIdAndUsername(Long companyId, String username);
    java.util.Optional<AppUser> findByCompanyIdAndUsername(Long companyId, String username);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from AppUser u join fetch u.company where u.company.id = :companyId and u.username = :username")
    java.util.Optional<AppUser> lockByCompanyIdAndUsername(Long companyId, String username);
    @org.springframework.data.jpa.repository.Query("select u from AppUser u join fetch u.company where u.id = :id")
    java.util.Optional<AppUser> findWithCompanyById(Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from AppUser u join fetch u.company where u.id = :id")
    java.util.Optional<AppUser> lockById(Long id);
    long countByCompanyIdAndRoleAndActiveTrue(Long companyId, com.myenterpriseos.myenterpriseopensource.enums.UserRole role);
    java.util.List<AppUser> findByCompanyIdOrderByIdAsc(Long companyId);
}
