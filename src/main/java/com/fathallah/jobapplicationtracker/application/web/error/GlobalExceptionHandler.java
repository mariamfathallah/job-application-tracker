package com.fathallah.jobapplicationtracker.application.web.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //404
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(EntityNotFoundException ex, HttpServletRequest request){
        return new ApiError(
                Instant.now(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    //403
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbidden(AccessDeniedException ex, HttpServletRequest request){
        return new ApiError(
                Instant.now(),
                403,
                "Forbidden",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    //401
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleUnauthorized(AuthenticationCredentialsNotFoundException ex,
                                       HttpServletRequest request){
        return new ApiError(
                Instant.now(),
                401,
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    //400 validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request){
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));

        return new ApiError(
                Instant.now(),
                400,
                "Bad Request",
                "Validation failed",
                request.getRequestURI(),
                errors
        );
    }

    //500 fallback
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGeneric(Exception ex, HttpServletRequest request){
        return new ApiError(
                Instant.now(),
                500,
                "Internal Server Error",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }
}
