package com.myenterpriseos.myenterpriseopensource.security;

import com.myenterpriseos.myenterpriseopensource.api.ApiException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;

public final class PasswordRules {
    private PasswordRules() {}

    public static void validate(String password) {
        if (password == null || password.length() < 14 ||
                password.getBytes(StandardCharsets.UTF_8).length > 72 ||
                password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 14 characters and at most 72 UTF-8 bytes");
        }
    }
}
