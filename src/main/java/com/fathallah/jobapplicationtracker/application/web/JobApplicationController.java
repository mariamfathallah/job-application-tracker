package com.fathallah.jobapplicationtracker.application.web;

import com.fathallah.jobapplicationtracker.application.domain.JobApplication;
import com.fathallah.jobapplicationtracker.application.service.JobApplicationService;
import com.fathallah.jobapplicationtracker.application.web.dto.*;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

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

    @GetMapping
    public PageResponse<JobApplicationDto> list(@PageableDefault(size = 20) @ParameterObject Pageable pageable){
        var page = service.getAllMine(pageable); //returns Page<JobApplication>
        return PageResponse.of(page.map(JobApplicationDto::from)); //or mapper
    }

    @GetMapping("/{id}")
    public JobApplicationDto getOne(@PathVariable Long id){

        return JobApplicationDto.from(service.findById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        service.deleteById(id);
    }

    @PatchMapping("/{id}/status")
    public JobApplicationDto updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req
            ){
        return JobApplicationDto.from(service.updateStatus(id, req.status()));
    }

    @PutMapping("/{id}")
    public JobApplicationDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobApplicationRequest request
            ){
        return JobApplicationDto.from(service.update(id, request));
    }
}
