package com.carsoffer.car.dto;

import com.carsoffer.common.exceptions.InvalidFuelTypeException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@Schema(description = "Type of fuel u lower or upper cases", enumeration = {"DIESEL", "PETROL", "ELECTRIC", "HYBRID"})
public enum FuelType {
    PETROL,
    DIESEL,
    ELECTRIC,
    HYBRID;

    private static final Logger log = LoggerFactory.getLogger(FuelType.class);

    @JsonCreator
    public static FuelType fromString(String key) {
        try {
            return FuelType.valueOf(key.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.error("Invalid fuel type provided: {}", key);
            throw new InvalidFuelTypeException(
                    "Invalid fuel type: " + key + ". Allowed values are: " + Arrays.toString(FuelType.values()));
        }
    }


    @JsonValue
    public String toString() {
        return this.name();
    }
}
