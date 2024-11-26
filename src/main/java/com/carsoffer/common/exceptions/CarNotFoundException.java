package com.carsoffer.common.exceptions;

public class CarNotFoundException extends RuntimeException {
    private final Long carId;

    public CarNotFoundException(Long carId) {
        super("Car with ID " + carId + " not found");
        this.carId = carId;
    }

    public Long getCarId() {
        return carId;
    }
}
