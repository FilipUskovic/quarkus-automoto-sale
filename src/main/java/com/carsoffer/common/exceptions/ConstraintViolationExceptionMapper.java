package com.carsoffer.common.exceptions;

import com.carsoffer.common.exceptions.dto.ErrorResponseValidation;
import com.carsoffer.common.exceptions.dto.ValidationErrors;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.stream.Collectors;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ValidationErrors> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ValidationErrors(
                        extractLastPathElement(violation.getPropertyPath().toString()),
                        violation.getMessage()))
                .distinct()
                .collect(Collectors.toList());

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponseValidation(errors))
                .build();
    }

    private String extractLastPathElement(String path) {
        String[] parts = path.split("\\.");
        return parts[parts.length - 1];
    }

}

