package com.fathallah.jobapplicationtracker.security.web;

import com.fathallah.jobapplicationtracker.security.TokenBlacklistService;
import com.fathallah.jobapplicationtracker.security.domain.RoleName;
import com.fathallah.jobapplicationtracker.security.domain.User;
import com.fathallah.jobapplicationtracker.security.repository.RoleRepository;
import com.fathallah.jobapplicationtracker.security.repository.UserRepository;
import com.fathallah.jobapplicationtracker.security.JwtService;
import com.fathallah.jobapplicationtracker.security.dto.AuthResponse;
import com.fathallah.jobapplicationtracker.security.dto.LoginRequest;
import com.fathallah.jobapplicationtracker.security.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@Tag(name = "Auth", description = "Register, log in, and log out")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(UserRepository users, RoleRepository roles, PasswordEncoder encoder, AuthenticationManager authManager, JwtService jwtService, TokenBlacklistService tokenBlacklistService){
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Operation(
            summary = "Register a new user account",
            description = "Creates a user and returns a JWT. Password must be ≥8 chars with uppercase, lowercase, digit, and special character.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered successfully — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Validation failure (weak password, blank fields)", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content),
            @ApiResponse(responseCode = "429", description = "Too many attempts — rate limited", content = @Content)
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req){
        if (users.existsByEmail(req.email())){
            log.warn("Registration failed, email already exists: {}", req.email());
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
        log.info("New user registered: {}", req.email());

        var roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String token = jwtService.generateToken(user.getEmail(), roleNames);
        return new AuthResponse(token);
    }

    @Operation(
            summary = "Log in and receive a JWT",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — JWT returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
            @ApiResponse(responseCode = "429", description = "Too many attempts — rate limited", content = @Content)
    })
    @PostMapping("/login")
    public  AuthResponse login(@Valid @RequestBody LoginRequest req){
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        var user = users.findByEmail(req.email()).orElseThrow();
        log.info("User logged in: {}", req.email());

        var roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String token = jwtService.generateToken(user.getEmail(), roleNames);

        return new AuthResponse(token);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String jti = jwtService.extractJti(header.substring(7));
            tokenBlacklistService.invalidate(jti);
            log.info("Token blacklisted on logout: jti={}", jti);
        }
    }


}
