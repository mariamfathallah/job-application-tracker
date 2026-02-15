package com.fathallah.jobapplicationtracker.security;

import com.fathallah.jobapplicationtracker.security.auth.DbUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final DbUserDetailsService userDetailsService;


    public JwtAuthFilter(JwtService jwtService, DbUserDetailsService userDetailsService){
        this.jwtService = jwtService;
        this.userDetailsService  = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain)
        throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")){
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = jwtService.parseAndValidate(token).getPayload();
            String email = claims.getSubject();

            var userDetails = userDetailsService.loadUserByUsername(email);

            var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e){
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(req, res);
    }
}
