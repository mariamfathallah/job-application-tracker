package com.fathallah.jobapplicationtracker.application.repository;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import com.fathallah.jobapplicationtracker.application.domain.JobApplication;
import org.springframework.data.jpa.domain.Specification;

public class JobApplicationSpecs {

    public static Specification<JobApplication> hasOwner(Long ownerId) {
        return (root, query, cb) ->
                cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<JobApplication> companyOrPositionContains(String q) {
        if (q == null || q.isBlank()) return ((root, query, cb) -> cb.conjunction());
        String pattern = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("company")), pattern),
                cb.like(cb.lower(root.get("position")), pattern)
        );
    }

    public static Specification<JobApplication> hasStatus(ApplicationStatus status) {
        if (status == null) return (root, query, cb) -> cb.conjunction();
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }
}
