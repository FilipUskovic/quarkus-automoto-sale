package com.carsoffer.car.dto;

import com.carsoffer.common.customvalidations.Alphanumeric;
import com.carsoffer.common.customvalidations.NotEmpty;
import com.carsoffer.common.utils.TrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record UpdateCarDTO(
        @NotEmpty(message = "Brand must not be empty")
        @Alphanumeric(message = "Model can contain alphanumeric characters, spaces, hyphens, and underscores, and must be between 1 and 50 characters", min = 1, max = 50, allowSpecialCharacters = true)
        @JsonDeserialize(using = TrimStringDeserializer.class)
        String brand,

        @NotEmpty(message = "Model must not be empty")
        @Alphanumeric(message = "Model can contain alphanumeric characters, spaces, hyphens, and underscores, and must be between 1 and 50 characters", min = 1, max = 50, allowSpecialCharacters = true)
        @JsonDeserialize(using = TrimStringDeserializer.class)
        String model,

        @Min(value = 1886, message = "Year must be no earlier than 1886")
        Integer year,

        @NotEmpty(message = "Color must not be empty")
        @Alphanumeric(message = "Color must be alphanumeric and between 1 and 30 characters", min = 1, max = 30)
        @JsonDeserialize(using = TrimStringDeserializer.class)
        String color,

        @NotNull(message = "Fuel type must not be null")
        @Schema(description = "Type of fuel. Valid values: DIESEL, PETROL, ELECTRIC, HYBRID", example = "DIESEL")
        FuelType fuelType
) {
}
