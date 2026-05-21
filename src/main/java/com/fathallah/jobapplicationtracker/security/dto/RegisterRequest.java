package com.fathallah.jobapplicationtracker.security.dto;

import com.fathallah.jobapplicationtracker.security.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String email,
        @NotBlank @ValidPassword String password,
        @NotBlank String displayName
) {
}
