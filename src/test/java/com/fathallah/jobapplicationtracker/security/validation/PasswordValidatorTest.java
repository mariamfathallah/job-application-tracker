package com.fathallah.jobapplicationtracker.security.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator   = new PasswordValidator();
    }

    @Test
    void null_shouldBeInvalid(){
        assertFalse(validator.isValid(null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short1A!",          // exactly 8 chars — valid boundary
            "ValidPass1!",       // normal valid password
            "UPPER1lower!",      // all conditions met
            "abcDEF1@",          // valid
    })
    void validPasswords_shouldReturnTrue(String password) {
        assertTrue(validator.isValid(password, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",               // too short
            "alllowercase1!",    // no uppercase
            "ALLUPPERCASE1!",    // no lowercase
            "NoDigitsHere!",     // no digit
            "NoSpecial1Char",    // no special character
            "short",             // too short and missing everything
            ""                   // empty
    })
    void invalidPasswords_shouldReturnFalse(String password) {
        assertFalse(validator.isValid(password, null));
    }
}
