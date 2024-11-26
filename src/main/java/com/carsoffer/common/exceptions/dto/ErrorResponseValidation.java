package com.carsoffer.common.exceptions.dto;

import java.util.List;

public class ErrorResponseValidation {
    private List<ValidationErrors> errors;

    public ErrorResponseValidation(List<ValidationErrors> errors) {
        this.errors = errors;
    }

    public List<ValidationErrors> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationErrors> errors) {
        this.errors = errors;
    }
}
