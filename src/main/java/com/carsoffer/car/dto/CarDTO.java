package com.carsoffer.car.dto;
public record CarDTO(
        Long id,
        String brand,
        String model,
        Integer year,
        String color,
        FuelType fuelType,
        String vin

) {
}
