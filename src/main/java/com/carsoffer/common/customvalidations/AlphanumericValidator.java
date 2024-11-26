package com.carsoffer.common.customvalidations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AlphanumericValidator implements ConstraintValidator<Alphanumeric, String> {

    private int min;
    private int max;
    private boolean allowSpecialCharacters;
    private boolean lettersOnly;

    @Override
    public void initialize(Alphanumeric constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
        this.allowSpecialCharacters = constraintAnnotation.allowSpecialCharacters();
        this.lettersOnly = constraintAnnotation.lettersOnly();

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        value = value.trim();

        String regex = lettersOnly
                ? "^[A-Ža-ž]+$"
                : (allowSpecialCharacters
                ? "^[A-Ža-ž0-9\\s-_]+$"
                : "^[A-Ža-ž0-9]+$");

        return value.matches(regex) && value.length() >= min && value.length() <= max;
    }


}
