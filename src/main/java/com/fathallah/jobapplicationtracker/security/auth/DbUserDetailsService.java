package com.fathallah.jobapplicationtracker.security.auth;

import com.fathallah.jobapplicationtracker.security.repository.UserRepository;
import lombok.NonNull;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DbUserDetailsService(UserRepository users){
        this.users = users;
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException{
        var u = users.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found " + username));

        return new UserPrincipal(u);
    }
}
