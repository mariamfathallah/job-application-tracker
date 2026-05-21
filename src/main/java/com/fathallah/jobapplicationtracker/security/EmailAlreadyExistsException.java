package com.fathallah.jobapplicationtracker.security;

public class EmailAlreadyExistsException extends RuntimeException{

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
