package com.carsoffer.common.customvalidations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidFormatValidator implements ConstraintValidator<ValidFormat, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.length() <= 50 && value.matches("^[A-Za-z0-9]+$");
    }
}
