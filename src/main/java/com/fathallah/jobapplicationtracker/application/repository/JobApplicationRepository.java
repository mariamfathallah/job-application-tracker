package com.fathallah.jobapplicationtracker.application.repository;

import com.fathallah.jobapplicationtracker.application.domain.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Page<JobApplication> findAllByOwner_Id(Long ownerId, Pageable pageable);

}
