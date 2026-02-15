package com.fathallah.jobapplicationtracker.security.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String displayName
) {
}
