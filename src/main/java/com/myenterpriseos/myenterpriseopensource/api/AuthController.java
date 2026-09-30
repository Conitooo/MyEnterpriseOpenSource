package com.myenterpriseos.myenterpriseopensource.api;

import com.myenterpriseos.myenterpriseopensource.dto.AuthDtos.*;
import com.myenterpriseos.myenterpriseopensource.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest body) { return service.login(body); }

    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest body) { return service.createUser(body); }

    @PostMapping("/users/change-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body) { service.changePassword(body); }

    @PostMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivate(@PathVariable Long userId) { return service.deactivate(userId); }
}
