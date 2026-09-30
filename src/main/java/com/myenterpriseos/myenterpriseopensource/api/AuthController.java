package com.myenterpriseos.myenterpriseopensource.api;

import com.myenterpriseos.myenterpriseopensource.dto.AuthDtos.*;
import com.myenterpriseos.myenterpriseopensource.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        request.setAttribute("audit.companyId", body.companyId());
        request.setAttribute("audit.username", body.username().trim());
        return service.login(body);
    }

    @GetMapping("/auth/me")
    public UserResponse me() { return service.me(); }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> users() { return service.users(); }

    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest body) { return service.createUser(body); }

    @PostMapping("/users/change-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body) { service.changePassword(body); }

    @PostMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivate(@PathVariable Long userId) { return service.deactivate(userId); }

    @PostMapping("/users/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse resetPassword(@PathVariable Long userId,
                                      @Valid @RequestBody ResetPasswordRequest body) {
        return service.resetPassword(userId, body);
    }
}
