package com.carsoffer.common.exceptions;

public class OfferNotFoundException extends RuntimeException{
    private final Long offerId;

    public OfferNotFoundException(Long offerId) {
        super("offer with ID " + offerId + " not found");
        this.offerId = offerId;
    }

    public Long getOfferId() {
        return offerId;
    }
}
