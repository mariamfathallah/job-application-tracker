package com.fathallah.jobapplicationtracker.application.web.dto;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import com.fathallah.jobapplicationtracker.application.domain.JobApplication;

import java.time.LocalDate;

public record JobApplicationDto(Long id, String company, String position, LocalDate dateApplied, ApplicationStatus status, String notes) {
    public static JobApplicationDto from(JobApplication app){
        return new JobApplicationDto(app.getId(), app.getCompany(), app.getPosition(), app.getDateApplied(), app.getStatus(), app.getNotes());
    }
}
