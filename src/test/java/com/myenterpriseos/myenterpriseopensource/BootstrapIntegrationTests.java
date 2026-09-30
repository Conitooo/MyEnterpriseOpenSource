package com.myenterpriseos.myenterpriseopensource;

import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.security.bootstrap.enabled=true",
        "BOOTSTRAP_COMPANY_NAME=Bootstrap Co",
        "BOOTSTRAP_ADMIN_USERNAME=bootstrap-admin",
        "BOOTSTRAP_ADMIN_PASSWORD=LongBootstrapPassword2026!",
        "spring.datasource.url=jdbc:h2:mem:bootstrap_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class BootstrapIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired CompanyRepository companies;

    @Test
    void firstAdministratorCanLogin() throws Exception {
        Long companyId = companies.findAll().getFirst().getId();
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"companyId\":" + companyId + ",\"username\":\"bootstrap-admin\",\"password\":\"LongBootstrapPassword2026!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
