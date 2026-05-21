package com.fathallah.jobapplicationtracker.security.dto;

import com.fathallah.jobapplicationtracker.security.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @Schema(description = "Email address", example = "john@example.com")
        @NotBlank String email,

        @Schema(description = "Password — min 8 chars, requires uppercase, lowercase, digit, special char", example = "Secur3!Pass")
        @NotBlank @ValidPassword String password,

        @Schema(description = "Name shown in the app", example = "John Doe")
        @NotBlank String displayName
) {
}
