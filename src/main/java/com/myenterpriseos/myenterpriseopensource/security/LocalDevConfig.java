package com.myenterpriseos.myenterpriseopensource.security;

import com.myenterpriseos.myenterpriseopensource.entity.AppUser;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.enums.UserRole;
import com.myenterpriseos.myenterpriseopensource.repository.AppUserRepository;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;

@Configuration
@Profile("local")
public class LocalDevConfig {
    @Bean
    KeyPair localJwtKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        return generator.generateKeyPair();
    }

    @Bean
    ApplicationRunner localAdminRunner(AppUserRepository users, CompanyRepository companies,
                                       PasswordEncoder encoder) {
        return new LocalAdminRunner(users, companies, encoder);
    }

    @Order(0)
    static class LocalAdminRunner implements ApplicationRunner {
        private final AppUserRepository users;
        private final CompanyRepository companies;
        private final PasswordEncoder encoder;

        LocalAdminRunner(AppUserRepository users, CompanyRepository companies, PasswordEncoder encoder) {
            this.users = users;
            this.companies = companies;
            this.encoder = encoder;
        }

        @Override
        @Transactional
        public void run(ApplicationArguments args) throws Exception {
            if (users.count() > 0) return;
            byte[] random = new byte[24];
            new SecureRandom().nextBytes(random);
            String password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            Company company = new Company();
            company.setName("Local demo");
            companies.save(company);
            AppUser admin = new AppUser();
            admin.setCompany(company);
            admin.setUsername("admin");
            admin.setPasswordHash(encoder.encode(password));
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);
            users.saveAndFlush(admin);
            Path folder = Path.of(".local");
            Files.createDirectories(folder);
            Path credentials = folder.resolve("credentials.txt");
            Files.writeString(credentials,
                    "Local development account\ncompanyId=" + company.getId() + "\nusername=admin\npassword=" + password + "\n",
                    StandardCharsets.UTF_8);
            System.out.println("Local admin credentials: " + credentials.toAbsolutePath());
        }
    }
}
