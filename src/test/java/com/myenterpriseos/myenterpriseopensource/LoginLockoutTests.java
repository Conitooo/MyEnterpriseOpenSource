package com.myenterpriseos.myenterpriseopensource;

import com.myenterpriseos.myenterpriseopensource.entity.AppUser;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.enums.UserRole;
import com.myenterpriseos.myenterpriseopensource.repository.AppUserRepository;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:lockout_test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class LoginLockoutTests {
    @Autowired MockMvc mvc;
    @Autowired CompanyRepository companies;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void failedAttemptsPersistAcrossTransactions() throws Exception {
        Company company = new Company(); company.setName("Lockout"); companies.save(company);
        AppUser user = new AppUser();
        user.setCompany(company);
        user.setUsername("person");
        user.setPasswordHash(passwords.encode("LongPasswordExample2026!"));
        user.setRole(UserRole.VIEWER);
        user.setActive(true);
        users.save(user);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content(body(company.getId(), "wrong"))).andExpect(status().isUnauthorized());
        }
        assertNotNull(users.findById(user.getId()).orElseThrow().getLockedUntil());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(body(company.getId(), "LongPasswordExample2026!"))).andExpect(status().isUnauthorized());
    }

    private String body(Long companyId, String password) {
        return "{\"companyId\":" + companyId + ",\"username\":\"person\",\"password\":\"" + password + "\"}";
    }
}
