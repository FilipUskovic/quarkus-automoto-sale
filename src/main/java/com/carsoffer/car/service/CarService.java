package com.carsoffer.car.service;

import com.carsoffer.car.dto.*;
import com.carsoffer.common.utils.PaginatedResponse;

import java.util.List;

public interface CarService {
    PaginatedResponse<CarDTO> getAllCars(int page, int size);

    CarDTO getCarById(Long id);

    CarWithOfferDTO getCarByIdWithOffers(Long id);

    CarDTO createCar(CreateCarDTO createCarDTO);

    CarDTO updateCar(Long id, UpdateCarDTO carDTO);

    void deleteCar(Long id);

    PaginatedResponse<CarDTO> findByBrandAndModel(String brand, String model, int page, int size);

    PaginatedResponse<CarDTO> findCarsByYearRange(int startYear, int endYear, int page, int size);

    List<CarDTO> searchCars(String brand, String model, Integer year, String color, FuelType fuelType, String sortBy, boolean asc, int page, int size);
}
