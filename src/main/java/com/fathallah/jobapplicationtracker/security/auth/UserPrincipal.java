package com.fathallah.jobapplicationtracker.security.auth;

import com.fathallah.jobapplicationtracker.security.domain.User;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;

    public Long getId(){ return user.getId(); }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities(){
        return user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                .toList();
    }

    @Override public @NonNull String getPassword() { return user.getPasswordHash(); }
    @Override public @NonNull String getUsername() { return user.getEmail(); }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
