package com.fathallah.jobapplicationtracker.application.web.dto;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import com.fathallah.jobapplicationtracker.application.domain.JobApplication;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "A job application belonging to the authenticated user")
public record JobApplicationDto(
        @Schema(example = "42") Long id,
        @Schema(example = "Google DeepMind") String company,
        @Schema(example = "Senior Software Engineer") String position,
        @Schema(example = "2026-01-15") LocalDate dateApplied,
        ApplicationStatus status,
        @Schema(description = "May be null if no notes were added") String notes
) {
    public static JobApplicationDto from(JobApplication app){
        return new JobApplicationDto(app.getId(), app.getCompany(), app.getPosition(), app.getDateApplied(), app.getStatus(), app.getNotes());
    }
}
