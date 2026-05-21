package com.fathallah.jobapplicationtracker.application.web.dto;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateJobApplicationRequest(
        @Schema(description = "Company name", example = "Google DeepMind")
        @NotBlank String company,

        @Schema(description = "Job title applied for", example = "Senior Software Engineer")
        @NotBlank String position,

        @Schema(description = "Current application status")
        @NotNull ApplicationStatus status,

        @Schema(description = "Date the application was submitted", example = "2026-01-15")
        @NotNull LocalDate dateApplied,

        @Schema(description = "Notes, interview details, contacts (optional)", example = "Referral from Jane, technical screen scheduled")
        String notes
        ) {
}
