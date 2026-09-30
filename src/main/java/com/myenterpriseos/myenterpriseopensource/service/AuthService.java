package com.myenterpriseos.myenterpriseopensource.service;

import com.myenterpriseos.myenterpriseopensource.api.ApiException;
import com.myenterpriseos.myenterpriseopensource.dto.AuthDtos.*;
import com.myenterpriseos.myenterpriseopensource.entity.AppUser;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.enums.UserRole;
import com.myenterpriseos.myenterpriseopensource.repository.AppUserRepository;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import com.myenterpriseos.myenterpriseopensource.security.PasswordRules;
import com.myenterpriseos.myenterpriseopensource.security.SecurityConfig;
import com.myenterpriseos.myenterpriseopensource.security.TenantGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final CompanyRepository companies;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final TenantGuard tenant;
    private final String issuer;
    private final String dummyHash;
    private final String registrationCode;

    public AuthService(AppUserRepository users, CompanyRepository companies, PasswordEncoder passwords,
                       JwtEncoder encoder, TenantGuard tenant,
                       @Value("${app.security.jwt.issuer}") String issuer,
                       @Value("${app.registration.code:}") String registrationCode) {
        this.users = users;
        this.companies = companies;
        this.passwords = passwords;
        this.encoder = encoder;
        this.tenant = tenant;
        this.issuer = issuer;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
        this.registrationCode = registrationCode;
        if (!registrationCode.isBlank() && registrationCode.length() < 24)
            throw new IllegalStateException("Registration code must have at least 24 characters");
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest body) {
        if (registrationCode.isBlank() || !MessageDigest.isEqual(
                registrationCode.getBytes(StandardCharsets.UTF_8),
                body.registrationCode().getBytes(StandardCharsets.UTF_8)))
            throw new ApiException(HttpStatus.FORBIDDEN, "Registration unavailable or invalid code");
        PasswordRules.validate(body.password());
        Company company = new Company();
        company.setName(body.companyName().trim());
        companies.saveAndFlush(company);
        AppUser admin = new AppUser();
        admin.setCompany(company);
        admin.setUsername(body.username().trim());
        admin.setPasswordHash(passwords.encode(body.password()));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        users.save(admin);
        return new RegistrationResponse(company.getId(), admin.getUsername());
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public TokenResponse login(LoginRequest body) {
        if (body.password().getBytes(StandardCharsets.UTF_8).length > 72) throw invalid();
        AppUser user = users.lockByCompanyIdAndUsername(body.companyId(), body.username().trim())
                .orElse(null);
        if (user == null) {
            passwords.matches(body.password(), dummyHash);
            throw invalid();
        }
        Instant now = Instant.now();
        if (!user.isActive() || (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil()))) {
            passwords.matches(body.password(), dummyHash);
            throw invalid();
        }
        if (!passwords.matches(body.password(), user.getPasswordHash())) {
            int failures = user.getFailedLoginAttempts() + 1;
            if (failures >= 5) {
                user.setLockedUntil(now.plusSeconds(900));
                user.setFailedLoginAttempts(0);
            } else user.setFailedLoginAttempts(failures);
            throw invalid();
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).subject(user.getId().toString())
                .audience(List.of(SecurityConfig.AUDIENCE))
                .issuedAt(now).expiresAt(now.plusSeconds(900))
                .id(UUID.randomUUID().toString())
                .claim("company_id", user.getCompany().getId())
                .claim("token_version", user.getTokenVersion())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return new TokenResponse(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(),
                "Bearer", 900);
    }

    private BadCredentialsException invalid() { return new BadCredentialsException("Invalid credentials"); }

    @Transactional
    public UserResponse createUser(CreateUserRequest body) {
        Long companyId = tenant.currentCompanyId();
        if (companyId == null) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        PasswordRules.validate(body.password());
        String username = body.username().trim();
        if (users.existsByCompanyIdAndUsername(companyId, username))
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists in company");
        AppUser user = new AppUser();
        user.setCompany(companies.findById(companyId).orElseThrow());
        user.setUsername(username);
        user.setPasswordHash(passwords.encode(body.password()));
        user.setRole(body.role());
        user.setActive(true);
        users.save(user);
        return response(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest body) {
        Long userId = tenant.currentUserId();
        AppUser user = users.lockById(userId).orElseThrow(this::invalid);
        if (body.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72) throw invalid();
        if (!passwords.matches(body.currentPassword(), user.getPasswordHash())) throw invalid();
        PasswordRules.validate(body.newPassword());
        if (passwords.matches(body.newPassword(), user.getPasswordHash()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password must differ from current password");
        user.setPasswordHash(passwords.encode(body.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    @Transactional
    public UserResponse deactivate(Long userId) {
        Long companyId = tenant.currentCompanyId();
        if (userId.equals(tenant.currentUserId()))
            throw new ApiException(HttpStatus.CONFLICT, "Cannot deactivate your own account");
        AppUser user = users.lockById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.getCompany().getId().equals(companyId))
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        companies.lockById(companyId).orElseThrow();
        if (user.isActive() && user.getRole() == UserRole.ADMIN &&
                users.countByCompanyIdAndRoleAndActiveTrue(companyId, UserRole.ADMIN) <= 1)
            throw new ApiException(HttpStatus.CONFLICT, "Cannot deactivate the last admin");
        user.setActive(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        return response(user);
    }

    @Transactional
    public UserResponse resetPassword(Long userId, ResetPasswordRequest body) {
        Long companyId = tenant.currentCompanyId();
        if (userId.equals(tenant.currentUserId()))
            throw new ApiException(HttpStatus.CONFLICT, "Use change-password for your own account");
        AppUser user = users.lockById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.getCompany().getId().equals(companyId))
            throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        PasswordRules.validate(body.newPassword());
        user.setPasswordHash(passwords.encode(body.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return response(user);
    }

    private UserResponse response(AppUser user) {
        return new UserResponse(user.getId(), user.getCompany().getId(), user.getUsername(),
                user.getRole(), user.isActive());
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        Long userId = tenant.currentUserId();
        if (userId == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return response(users.findWithCompanyById(userId).orElseThrow(this::invalid));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> users() {
        Long companyId = tenant.currentCompanyId();
        if (companyId == null) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        return users.findByCompanyIdOrderByIdAsc(companyId).stream().map(this::response).toList();
    }
}
