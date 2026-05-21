package com.fathallah.jobapplicationtracker.security.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    public boolean isValid(String value, ConstraintValidatorContext context){
        if (value == null) return false;
        return value.length() >= 8
                && value.chars().anyMatch(Character::isUpperCase)
                && value.chars().anyMatch(Character::isLowerCase)
                && value.chars().anyMatch(Character::isDigit)
                && value.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\\\",./<>?".indexOf(c) >= 0);
    }
}
