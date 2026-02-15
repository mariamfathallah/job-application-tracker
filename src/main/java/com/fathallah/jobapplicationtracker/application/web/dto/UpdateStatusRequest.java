package com.fathallah.jobapplicationtracker.application.web.dto;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull ApplicationStatus status
        ) {
}
