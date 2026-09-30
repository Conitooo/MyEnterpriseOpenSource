package com.myenterpriseos.myenterpriseopensource.dto;

import com.myenterpriseos.myenterpriseopensource.enums.UserRole;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotNull @Positive Long companyId,
                               @NotBlank @Size(max = 255) String username,
                               @NotBlank String password) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
    public record RegisterRequest(@NotBlank @Size(max = 255) String companyName,
                                  @NotBlank @Size(max = 255) String username,
                                  @NotBlank String password,
                                  @NotBlank String registrationCode) {}
    public record RegistrationResponse(Long companyId, String username) {}
    public record CreateUserRequest(@NotBlank @Size(max = 255) String username,
                                    @NotBlank String password,
                                    @NotNull UserRole role) {}
    public record UserResponse(Long id, Long companyId, String username, UserRole role, boolean active) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank String newPassword) {}
    public record ResetPasswordRequest(@NotBlank String newPassword) {}
}
