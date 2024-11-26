package com.carsoffer.common.exceptions;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
@Provider
public class ValueInstantiationExceptionMapper implements ExceptionMapper<ValueInstantiationException> {

    private static final Logger log = LoggerFactory.getLogger(ValueInstantiationExceptionMapper.class);

    @Override
    public Response toResponse(ValueInstantiationException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof InvalidFuelTypeException) {
            log.warn("InvalidFuelTypeException caught during deserialization: {}", cause.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid fuel type", "details", cause.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        log.error("ValueInstantiationException caught: {}", exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Invalid input", "details", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
