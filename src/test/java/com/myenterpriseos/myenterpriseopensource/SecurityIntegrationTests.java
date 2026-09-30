package com.myenterpriseos.myenterpriseopensource;

import com.myenterpriseos.myenterpriseopensource.dto.AuthDtos.LoginRequest;
import com.myenterpriseos.myenterpriseopensource.entity.AppUser;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.enums.UserRole;
import com.myenterpriseos.myenterpriseopensource.repository.AppUserRepository;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import com.myenterpriseos.myenterpriseopensource.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTests {
    private static final String PASSWORD = "ExamplePassword2026!";
    @Autowired MockMvc mvc;
    @Autowired CompanyRepository companies;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthService auth;
    @Autowired JwtEncoder jwtEncoder;
    private Long first;
    private Long second;
    private Long viewerId;

    @BeforeEach
    void setUp() {
        Company a = new Company(); a.setName("First"); first = companies.save(a).getId();
        Company b = new Company(); b.setName("Second"); second = companies.save(b).getId();
        user(first, "admin", UserRole.ADMIN);
        viewerId = user(first, "viewer", UserRole.VIEWER);
        user(first, "sales", UserRole.SALES);
        user(second, "other", UserRole.ADMIN);
    }

    private Long user(Long companyId, String username, UserRole role) {
        AppUser user = new AppUser();
        user.setCompany(companies.findById(companyId).orElseThrow());
        user.setUsername(username);
        user.setPasswordHash(passwords.encode(PASSWORD));
        user.setRole(role);
        user.setActive(true);
        return users.save(user).getId();
    }

    private String token(Long companyId, String username) {
        return "Bearer " + auth.login(new LoginRequest(companyId, username, PASSWORD)).accessToken();
    }

    @Test
    void anonymousAndInvalidTokensAreRejected() throws Exception {
        mvc.perform(get("/api/companies/{id}/products", first)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/companies/{id}/products", first)
                .header("Authorization", "Bearer invalid.jwt.value")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginAndValidationWorkOverHttp() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"companyId\":" + first + ",\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"companyId\":" + first + ",\"username\":\"admin\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"companyId\":" + first + ",\"username\":\"\",\"password\":\"bad\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/companies/{id}/orders", first).header("Authorization", token(first, "sales"))
                        .contentType("application/json")
                        .content("{\"items\":[{\"productId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rolesAndCompanyBoundariesAreEnforced() throws Exception {
        mvc.perform(get("/api/companies/{id}/products", first)
                .header("Authorization", token(first, "viewer"))).andExpect(status().isOk());
        mvc.perform(get("/api/companies/{id}/products", second)
                .header("Authorization", token(first, "viewer"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/companies/{id}/products", first)
                        .header("Authorization", token(first, "viewer"))
                        .contentType("application/json")
                        .content("{\"productName\":\"Widget\",\"sku\":\"X\",\"price\":1,\"currency\":\"EUR\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/companies/{id}/products", first)
                        .header("Authorization", token(first, "admin"))
                        .contentType("application/json")
                        .content("{\"productName\":\"Widget\",\"sku\":\"X\",\"price\":1,\"currency\":\"EUR\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void passwordChangeRevokesExistingToken() throws Exception {
        String oldToken = token(first, "viewer");
        mvc.perform(post("/api/users/change-password").header("Authorization", oldToken)
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"AnotherPassword2026!\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/companies/{id}/products", first)
                .header("Authorization", oldToken)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"companyId\":" + first + ",\"username\":\"viewer\",\"password\":\"AnotherPassword2026!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deactivationRevokesToken() throws Exception {
        String viewerToken = token(first, "viewer");
        mvc.perform(post("/api/users/{id}/deactivate", viewerId)
                .header("Authorization", token(first, "admin"))).andExpect(status().isOk());
        mvc.perform(get("/api/companies/{id}/products", first)
                .header("Authorization", viewerToken)).andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedBadPasswordsLockAccount() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType("application/json")
                            .content("{\"companyId\":" + first + ",\"username\":\"viewer\",\"password\":\"bad\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"companyId\":" + first + ",\"username\":\"viewer\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredAndWrongAudienceTokensAreRejected() throws Exception {
        Instant now = Instant.now();
        String expired = signedToken("myenterpriseos-api", first, now.minusSeconds(3600), now.minusSeconds(1800));
        String wrongAudience = signedToken("another-api", first, now, now.plusSeconds(900));
        String wrongCompany = signedToken("myenterpriseos-api", second, now, now.plusSeconds(900));
        String excessiveLifetime = signedToken("myenterpriseos-api", first, now, now.plusSeconds(3600));
        mvc.perform(get("/api/companies/{id}/products", first)
                .header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/companies/{id}/products", first)
                .header("Authorization", "Bearer " + wrongAudience)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/companies/{id}/products", first)
                .header("Authorization", "Bearer " + wrongCompany)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/companies/{id}/products", first)
                .header("Authorization", "Bearer " + excessiveLifetime)).andExpect(status().isUnauthorized());
    }

    private String signedToken(String audience, Long companyId, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("https://test.myenterpriseos.local")
                .subject(viewerId.toString()).audience(List.of(audience))
                .issuedAt(issuedAt).expiresAt(expiresAt)
                .id("security-test-token")
                .claim("company_id", companyId).claim("token_version", 0).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    @Test
    void onlyAdminCanCreateUsers() throws Exception {
        String body = "{\"username\":\"new-user\",\"password\":\"AnotherPassword2026!\",\"role\":\"VIEWER\"}";
        mvc.perform(post("/api/users").header("Authorization", token(first, "sales"))
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/users").header("Authorization", token(first, "admin"))
                .contentType("application/json").content(body)).andExpect(status().isCreated());
    }

    @Test
    void newlyCreatedUserCanLogInAndReadOwnProfile() throws Exception {
        String username = " new-viewer ";
        mvc.perform(post("/api/users").header("Authorization", token(first, "admin"))
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("new-viewer"));
        String accessToken = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"companyId\":" + first + ",\"username\":\" new-viewer \",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + accessToken.replaceAll(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");
        mvc.perform(get("/api/auth/me").header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("new-viewer"))
                .andExpect(jsonPath("$.companyId").value(first));
    }

    @Test
    void adminCanResetLostPasswordAndRevokeOldToken() throws Exception {
        String oldToken = token(first, "viewer");
        String body = "{\"newPassword\":\"ReplacementPassword2026!\"}";
        mvc.perform(post("/api/users/{id}/reset-password", viewerId)
                        .header("Authorization", token(first, "sales"))
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/users/{id}/reset-password", viewerId)
                        .header("Authorization", token(second, "other"))
                        .contentType("application/json").content(body))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/users/{id}/reset-password", viewerId)
                        .header("Authorization", token(first, "admin"))
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("viewer"));
        mvc.perform(get("/api/auth/me").header("Authorization", oldToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"companyId\":" + first + ",\"username\":\"viewer\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"companyId\":" + first + ",\"username\":\"viewer\",\"password\":\"ReplacementPassword2026!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void auditEventsAreTenantScopedAndAdminOnly() throws Exception {
        String admin = token(first, "admin");
        mvc.perform(post("/api/companies/{id}/products", first)
                        .header("Authorization", admin)
                        .contentType("application/json")
                        .content("{\"productName\":\"Audited\",\"sku\":\"AUDIT-1\",\"price\":1,\"currency\":\"EUR\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("X-Request-Id"));
        mvc.perform(get("/api/audit-events").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.events[0].username").value("admin"))
                .andExpect(jsonPath("$.events[0].route").value("/api/companies/{companyId}/products"));
        mvc.perform(get("/api/audit-events").header("Authorization", token(first, "viewer")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/audit-events").header("Authorization", token(second, "other")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void frontendReadEndpointsRespectAuthenticationAndTenant() throws Exception {
        String admin = token(first, "admin");
        String viewer = token(first, "viewer");
        mvc.perform(get("/api/auth/me").header("Authorization", viewer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.companyId").value(first));
        mvc.perform(get("/api/users").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("admin"));
        mvc.perform(get("/api/users").header("Authorization", viewer))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/companies/{id}/orders", first).header("Authorization", viewer))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/companies/{id}/orders", second).header("Authorization", viewer))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/{id}/shipments", 99999).header("Authorization", viewer))
                .andExpect(status().isForbidden());
    }
}
