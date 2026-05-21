package com.fathallah.jobapplicationtracker.application.web;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import com.fathallah.jobapplicationtracker.application.domain.JobApplication;
import com.fathallah.jobapplicationtracker.application.service.JobApplicationService;
import com.fathallah.jobapplicationtracker.application.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Applications", description = "Create, read, update, and delete job applications")
@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "Create a new job application")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application created"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplicationDto create(@Valid @RequestBody CreateJobApplicationRequest req) {
        JobApplication app = JobApplication.builder()
                .company(req.company())
                .position(req.position())
                .status(req.status())
                .dateApplied(req.dateApplied())
                .notes(req.notes())
                .build();

        return JobApplicationDto.from(service.create(app));
    }

    @Operation(
            summary = "List my applications",
            description = "Paginated list of the authenticated user's applications. Supports search, status filter, and sorting."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of applications"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @GetMapping
    public PageResponse<JobApplicationDto> list(
            @Parameter(description = "Search by company or position name", example = "Google")
            @RequestParam(required = false) String q,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20) @ParameterObject Pageable pageable){
        var page = service.getAllMine(q, status, pageable); //returns Page<JobApplication>
        return PageResponse.of(page.map(JobApplicationDto::from)); //or mapper
    }

    @Operation(summary = "Get a single application by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found or not yours", content = @Content)
    })
    @GetMapping("/{id}")
    public JobApplicationDto getOne(@PathVariable Long id){

        return JobApplicationDto.from(service.findById(id));
    }

    @Operation(summary = "Delete an application")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found or not yours", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        service.deleteById(id);
    }

    @Operation(summary = "Update the status of an application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status value", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found or not yours", content = @Content)
    })
    @PatchMapping("/{id}/status")
    public JobApplicationDto updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req
            ){
        return JobApplicationDto.from(service.updateStatus(id, req.status()));
    }

    @Operation(summary = "Replace all fields of an application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application updated"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found or not yours", content = @Content)
    })
    @PutMapping("/{id}")
    public JobApplicationDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobApplicationRequest request
            ){
        return JobApplicationDto.from(service.update(id, request));
    }
}
