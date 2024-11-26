package com.carsoffer.offer.dto;

import java.time.LocalDate;

public record OfferSearchCriteria(
        String customerFirstName,
        String customerLastName,
        Double minPrice,
        Double maxPrice,
        LocalDate startDate,
        LocalDate endDate,
        String sortBy,
        boolean asc,
        int page,
        int size) {


    public OfferSearchCriteria {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "id";
        }
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page and size must be valid positive numbers.");
        }
    }


}

