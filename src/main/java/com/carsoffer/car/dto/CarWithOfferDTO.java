package com.carsoffer.car.dto;

import com.carsoffer.offer.dto.OfferDTO;

import java.util.Set;

public record CarWithOfferDTO(
        Long id,
        String brand,
        String model,
        int year,
        String color,
        FuelType fuelType,
        String vin,
        Set<OfferDTO> offers

) {
}
