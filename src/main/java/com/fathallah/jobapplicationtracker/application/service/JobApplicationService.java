package com.fathallah.jobapplicationtracker.application.service;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import com.fathallah.jobapplicationtracker.application.domain.JobApplication;
import com.fathallah.jobapplicationtracker.application.repository.JobApplicationRepository;
import com.fathallah.jobapplicationtracker.security.auth.CurrentUser;
import com.fathallah.jobapplicationtracker.security.repository.UserRepository;
import com.fathallah.jobapplicationtracker.application.web.dto.UpdateJobApplicationRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return repository.save(application);
    }

    @Transactional(readOnly = true)
    public Page<JobApplication> getAllMine(Pageable pageable) {
        return repository.findAllByOwner_Id(currentUser.id(), pageable);
    }

    private JobApplication requireOwner(Long id) {
        var app = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + id));
        if (!app.getOwner().getId().equals(currentUser.id())) {
            throw new org.springframework.security.access.AccessDeniedException("Not owner");
        }
        return app;
    }

    @Transactional(readOnly = true)
    public JobApplication findById(Long id) {
        return requireOwner(id);
    }

    public void deleteById(Long id){
        repository.delete(requireOwner(id));
    }

    public JobApplication updateStatus(Long id, ApplicationStatus status){
        var app = requireOwner(id);
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
