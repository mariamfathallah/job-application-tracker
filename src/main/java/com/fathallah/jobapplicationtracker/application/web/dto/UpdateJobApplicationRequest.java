package com.fathallah.jobapplicationtracker.application.web.dto;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateJobApplicationRequest(
        @NotBlank String company,
        @NotBlank String position,
        @NotNull ApplicationStatus status,
        @NotNull LocalDate dateApplied,
        String notes
        ) {
}
