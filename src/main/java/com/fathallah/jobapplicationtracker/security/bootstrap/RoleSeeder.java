package com.fathallah.jobapplicationtracker.security.bootstrap;

import com.fathallah.jobapplicationtracker.security.domain.Role;
import com.fathallah.jobapplicationtracker.security.domain.RoleName;
import com.fathallah.jobapplicationtracker.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepo;

    @Override
    public void run(String @NonNull ... args){
        for (RoleName rn : RoleName.values()){
            roleRepo.findByName(rn).orElseGet(() ->
                    roleRepo.save(Role.builder().name(rn).build())
            );
        }
    }
}
