package com.fathallah.jobapplicationtracker.security.web;

import com.fathallah.jobapplicationtracker.security.domain.RoleName;
import com.fathallah.jobapplicationtracker.security.domain.User;
import com.fathallah.jobapplicationtracker.security.repository.RoleRepository;
import com.fathallah.jobapplicationtracker.security.repository.UserRepository;
import com.fathallah.jobapplicationtracker.security.JwtService;
import com.fathallah.jobapplicationtracker.security.dto.AuthResponse;
import com.fathallah.jobapplicationtracker.security.dto.LoginRequest;
import com.fathallah.jobapplicationtracker.security.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthController(UserRepository users, RoleRepository roles, PasswordEncoder encoder, AuthenticationManager authManager, JwtService jwtService){
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req){
        if (users.existsByEmail(req.email())){
            throw new IllegalArgumentException("Email already exists");
        }

        var userRole = roles.findByName(RoleName.ROLE_USER).orElseThrow();

        var user = User.builder()
                .email(req.email())
                .displayName(req.displayName())
                .passwordHash(encoder.encode(req.password()))
                .roles(Set.of(userRole))
                .build();

        users.save(user);

        var roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String token = jwtService.generateToken(user.getEmail(), roleNames);
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public  AuthResponse login(@Valid @RequestBody LoginRequest req){
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        var user = users.findByEmail(req.email()).orElseThrow();

        var roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String token = jwtService.generateToken(user.getEmail(), roleNames);

        return new AuthResponse(token);
    }


}
