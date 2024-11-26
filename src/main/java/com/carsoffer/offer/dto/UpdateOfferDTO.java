package com.carsoffer.offer.dto;

import com.carsoffer.common.customvalidations.Alphanumeric;
import com.carsoffer.common.customvalidations.NotEmpty;
import com.carsoffer.common.utils.TrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;


public record UpdateOfferDTO(
        @NotEmpty(message = "FirstName must not be empty")
        @Alphanumeric(lettersOnly = true, message = "FirstName must contain only letters")
        @JsonDeserialize(using = TrimStringDeserializer.class)

        String customerFirstName,
        @NotEmpty(message = "LastName must not be empty")
        @Alphanumeric(lettersOnly = true, message = "LastName must contain only letters")
        @JsonDeserialize(using = TrimStringDeserializer.class)

        String customerLastName,
        @Positive(message = "Price must be positive")
        @NotNull(message = "Price must not be null")
        BigDecimal price,
        @NotNull(message = "Car ID must be selected")
        Long carId
) {
}
