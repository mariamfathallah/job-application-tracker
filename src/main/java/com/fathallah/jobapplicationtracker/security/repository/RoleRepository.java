package com.fathallah.jobapplicationtracker.security.repository;

import com.fathallah.jobapplicationtracker.security.domain.Role;
import com.fathallah.jobapplicationtracker.security.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
