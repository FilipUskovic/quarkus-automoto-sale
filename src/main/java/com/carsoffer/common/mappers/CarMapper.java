package com.carsoffer.common.mappers;

import com.carsoffer.car.dto.CarDTO;
import com.carsoffer.car.dto.CarWithOfferDTO;
import com.carsoffer.car.entity.Car;
import com.carsoffer.offer.dto.OfferDTO;

import java.util.Set;
import java.util.stream.Collectors;

public class CarMapper {

    public static CarDTO toDTO(Car car) {
        if (car == null) {
            return null;
        }

        return new CarDTO(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getYear(),
                car.getColor(),
                car.getFuelType(),
                car.getVin()

        );

    }

    public static CarWithOfferDTO toDTOWithOffers(Car car) {
        if (car == null) {
            return null;
        }
        Set<OfferDTO> offerDTOs = car.getOffers().stream()
                .map(OfferMapper::toDTOWithoutCar)
                .collect(Collectors.toSet());

        return new CarWithOfferDTO(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getYear(),
                car.getColor(),
                car.getFuelType(),
                car.getVin(),
                offerDTOs
        );
    }


    public static Car toEntity(CarDTO carDTO) {
        if (carDTO == null) {
            return null;
        }



        return new Car.Builder()
                .id(carDTO.id())
                .brand(carDTO.brand())
                .model(carDTO.model())
                .year(carDTO.year())
                .color(carDTO.color())
                .fuelType(carDTO.fuelType())
                .vin(carDTO.vin())
                .build();
    }
}
