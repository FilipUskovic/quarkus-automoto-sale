package com.carsoffer.offer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OfferDTO(
        Long id,
        String customerFirstName,
        String customerLastName,
        BigDecimal price,
        LocalDateTime offerDate,
        LocalDateTime updatedOffer,
        Long carId
) {
}
