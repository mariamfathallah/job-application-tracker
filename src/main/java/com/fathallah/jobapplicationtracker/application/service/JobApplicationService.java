package com.fathallah.jobapplicationtracker.application.service;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import com.fathallah.jobapplicationtracker.application.domain.JobApplication;
import com.fathallah.jobapplicationtracker.application.repository.JobApplicationRepository;
import com.fathallah.jobapplicationtracker.application.repository.JobApplicationSpecs;
import com.fathallah.jobapplicationtracker.security.auth.CurrentUser;
import com.fathallah.jobapplicationtracker.security.repository.UserRepository;
import com.fathallah.jobapplicationtracker.application.web.dto.UpdateJobApplicationRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JobApplicationService {

    private final JobApplicationRepository repository;
    private final UserRepository userRepo;
    private final CurrentUser currentUser;

    public JobApplicationService(JobApplicationRepository repository,
                                 UserRepository userRepo,
                                 CurrentUser currentUser) {
        this.repository = repository;
        this.userRepo = userRepo;
        this.currentUser = currentUser;
    }

    @Transactional
    public JobApplication create(JobApplication application) {
        var owner = userRepo.getReferenceById(currentUser.id());
        application.setOwner(owner);
        var saved = repository.save(application);
        log.info("User {} created application {} for '{}'", currentUser.id(),
                saved.getId(), saved.getCompany());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> getAllMine(String query, ApplicationStatus status, Pageable pageable) {
        var spec = Specification
                .where(JobApplicationSpecs.hasOwner(currentUser.id()))
                .and(JobApplicationSpecs.companyOrPositionContains(query))
                .and(JobApplicationSpecs.hasStatus(status));
        return repository.findAll(spec, pageable);
    }

    private JobApplication requireOwner(Long id) {
        var app = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Application {} not found", id);
                    return new EntityNotFoundException("Application not found: " + id);
                });
        if (!app.getOwner().getId().equals(currentUser.id())) {
            log.warn("User {} attempted to access application {} owned by {}", currentUser.id(), id, app.getOwner().getId());
            throw new AccessDeniedException("Not owner");
        }
        return app;
    }

    @Transactional(readOnly = true)
    public JobApplication findById(Long id) {
        return requireOwner(id);
    }

    public void deleteById(Long id){
        var app = requireOwner(id);
        repository.delete(app);
        log.info("User {} deleted application {}", currentUser.id(), id);

    }

    public JobApplication updateStatus(Long id, ApplicationStatus status){
        var app = requireOwner(id);
        log.info("User {} changed application {} status to {}", currentUser.id(), id, status);
        app.setStatus(status);
        return repository.save(app);
    }

    public JobApplication update(Long id, UpdateJobApplicationRequest req){
        var app = requireOwner(id);
        app.setCompany(req.company());
        app.setPosition(req.position());
        app.setStatus(req.status());
        app.setDateApplied(req.dateApplied());
        app.setNotes(req.notes());

        return repository.save(app);
    }
}
