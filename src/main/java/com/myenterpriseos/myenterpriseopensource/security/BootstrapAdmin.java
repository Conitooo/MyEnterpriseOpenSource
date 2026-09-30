package com.myenterpriseos.myenterpriseopensource.security;

import com.myenterpriseos.myenterpriseopensource.entity.AppUser;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.enums.UserRole;
import com.myenterpriseos.myenterpriseopensource.repository.AppUserRepository;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.security.bootstrap.enabled", havingValue = "true")
public class BootstrapAdmin implements ApplicationRunner {
    private final AppUserRepository users;
    private final CompanyRepository companies;
    private final PasswordEncoder passwords;
    private final String companyId;
    private final String companyName;
    private final String username;
    private final String password;

    public BootstrapAdmin(AppUserRepository users, CompanyRepository companies, PasswordEncoder passwords,
                          @Value("${BOOTSTRAP_COMPANY_ID:}") String companyId,
                          @Value("${BOOTSTRAP_COMPANY_NAME:}") String companyName,
                          @Value("${BOOTSTRAP_ADMIN_USERNAME:}") String username,
                          @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.companies = companies;
        this.passwords = passwords;
        this.companyId = companyId;
        this.companyName = companyName;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        if (username.isBlank() || username.length() > 255) throw new IllegalStateException("Set BOOTSTRAP_ADMIN_USERNAME");
        PasswordRules.validate(password);
        Company company;
        if (!companyId.isBlank()) {
            company = companies.findById(Long.valueOf(companyId))
                    .orElseThrow(() -> new IllegalStateException("Bootstrap company ID not found"));
        } else {
            if (companyName.isBlank() || companyName.length() > 255)
                throw new IllegalStateException("Set BOOTSTRAP_COMPANY_NAME or BOOTSTRAP_COMPANY_ID");
            company = new Company();
            company.setName(companyName.trim());
            companies.save(company);
        }
        AppUser admin = new AppUser();
        admin.setCompany(company);
        admin.setUsername(username.trim());
        admin.setPasswordHash(passwords.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        users.save(admin);
    }
}
