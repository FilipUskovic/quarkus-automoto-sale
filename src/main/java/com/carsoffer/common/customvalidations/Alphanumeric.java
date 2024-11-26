package com.carsoffer.common.customvalidations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AlphanumericValidator.class)
public @interface Alphanumeric {
    String message() default "Field must be alphanumeric and between 1 and 50 characters";
    int min() default 1;
    int max() default 50;
    boolean lettersOnly() default false;
    boolean allowSpecialCharacters() default false;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
