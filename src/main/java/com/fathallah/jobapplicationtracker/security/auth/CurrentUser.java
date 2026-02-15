package com.fathallah.jobapplicationtracker.security.auth;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public Long id(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !(auth.getPrincipal() instanceof UserPrincipal up)){
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }
        return up.getId();
    }
}
