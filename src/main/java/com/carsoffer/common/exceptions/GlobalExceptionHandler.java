package com.carsoffer.common.exceptions;

import com.carsoffer.common.exceptions.dto.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.LoggerFactory;

import java.util.Map;


@Provider
@ApplicationScoped
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public Response toResponse(Throwable exception) {
        log.info("Handling exception: {}", exception.getClass().getName(), exception);

        return switch (exception) {
            case CarNotFoundException carNotFoundException -> handleCarNotFoundException(carNotFoundException);
            case OfferNotFoundException offerNotFoundException -> handleOfferNotFoundException(offerNotFoundException);
            case DuplicateCarException duplicateCarException -> handleDuplicateCarException(duplicateCarException);
            case IllegalArgumentException illegalArgumentException -> handleIllegalArgumentException(illegalArgumentException);
            case EntityNotFoundException entityNotFoundException -> handleEntityNotFoundException(entityNotFoundException);
            case WebApplicationException webAppException -> handleWebApplicationException(webAppException);
            default -> handleGenericException(exception);
        };
    }


    private Response handleWebApplicationException(WebApplicationException exception) {
        log.warn("WebApplicationException caught: {}", exception.getMessage());

        Throwable cause = exception.getCause();
        if (cause != null) {
            Throwable valueCause = cause.getCause();
            if (valueCause instanceof InvalidFuelTypeException) {
                String details = valueCause.getMessage();
                log.warn("InvalidFuelTypeException caught during deserialization: {}", details);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid fuel type", "details", details))
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
        }

        return Response.status(exception.getResponse().getStatus())
                .entity(Map.of("error", "Bad Request", "details", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }


    private Response handleCarNotFoundException(CarNotFoundException exception) {
        log.warn("Car not found: ID = {}", exception.getCarId());
        ErrorResponse errorResponse = new ErrorResponse(
                "Car not found",
                "Car with ID " + exception.getCarId() + " not found"
        );
        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleOfferNotFoundException(OfferNotFoundException exception) {
        log.warn("Offer not found: ID = {}", exception.getOfferId());
        ErrorResponse errorResponse = new ErrorResponse(
                "Offer not found",
                "Offer with ID " + exception.getOfferId() + " not found"
        );
        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleDuplicateCarException(DuplicateCarException exception) {
        log.warn("Duplicate car creation attempt: {}", exception.getMessage());
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("Conflict", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }


    private Response handleEntityNotFoundException(EntityNotFoundException exception) {
        return buildErrorResponse("Entity not found", exception.getMessage(), Response.Status.NOT_FOUND);
    }

    private Response handleIllegalArgumentException(IllegalArgumentException exception) {
        return buildErrorResponse("Invalid argument", exception.getMessage(), Response.Status.BAD_REQUEST);
    }

    private Response handleGenericException(Throwable exception) {
        log.error("Unhandled exception occurred", exception);
        return buildErrorResponse("Internal server error", "An unexpected error occurred", Response.Status.INTERNAL_SERVER_ERROR);
    }

    private Response buildErrorResponse(String message, String details, Response.Status status) {
        ErrorResponse errorResponse = new ErrorResponse(message, details);
        return Response.status(status)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
