package com.carsoffer.common.mappers;

import com.carsoffer.car.entity.Car;
import com.carsoffer.offer.dto.OfferDTO;
import com.carsoffer.offer.entity.Offer;

public class OfferMapper {

    public static OfferDTO toDTO(Offer offer) {
        if (offer == null) {
            return null;
        }
        return new OfferDTO(
                offer.getId(),
                offer.getCustomerFirstName(),
                offer.getCustomerLastName(),
                offer.getPrice(),
                offer.getOfferDate(),
                offer.getLastModifiedOffer(),
                offer.getCar() != null ? offer.getCar().getId() : null

        );
    }

    public static OfferDTO toDTOWithoutCar(Offer offer) {
        if (offer == null) {
            return null;
        }
        return new OfferDTO(
                offer.getId(),
                offer.getCustomerFirstName(),
                offer.getCustomerLastName(),
                offer.getPrice(),
                offer.getOfferDate(),
                offer.getLastModifiedOffer(),
                null // Exclude carId to prevent circular reference

        );
    }

    public static Offer toEntity(OfferDTO offerDTO, Car car) {
        if (offerDTO == null || car == null) {
            return null;
        }
        return new Offer.Builder()
                .id(offerDTO.id())
                .car(car)
                .customerFirstName(offerDTO.customerFirstName())
                .customerLastName(offerDTO.customerLastName())
                .price(offerDTO.price())
                .offerDate(offerDTO.offerDate())
                .lastModifiedDate(offerDTO.updatedOffer())
                .build();
    }
}
