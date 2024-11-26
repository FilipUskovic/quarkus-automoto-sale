package com.carsoffer.car.service;

import com.carsoffer.car.dto.*;
import com.carsoffer.car.entity.Car;
import com.carsoffer.car.repository.CarRepository;
import com.carsoffer.common.exceptions.CarNotFoundException;
import com.carsoffer.common.exceptions.DuplicateCarException;
import com.carsoffer.common.mappers.CarMapper;
import com.carsoffer.common.utils.PaginatedResponse;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CarServiceImpl implements CarService {

    private static final Logger log = LoggerFactory.getLogger(CarServiceImpl.class);
    private final CarRepository carRepository;

    @Inject
    public CarServiceImpl(CarRepository carRepository) {
        this.carRepository = carRepository;
    }


    @Override
    public PaginatedResponse<CarDTO> getAllCars(int page, int size) {
        log.info("Fetching all cars - page {}, size {}", page, size);
        PanacheQuery<Car> query = carRepository.findAllPaged(page, size);
        List<CarDTO> carDTOs = query.stream()
                .map(CarMapper::toDTO)
                .collect(Collectors.toList());
        return new PaginatedResponse<>(carDTOs, query.count(), query.pageCount(), page, size);
    }

    @Override
    @CacheResult(cacheName = "car-cache")
    public CarDTO getCarById(Long id) {
        log.info("Fetching car by ID: {}", id);
        return carRepository.findByIdOptional(id)
                .map(CarMapper::toDTO)
                .orElseThrow(() -> new CarNotFoundException(id));
    }

    @Override
    @CacheResult(cacheName = "car-offers-cache")
    @Transactional(Transactional.TxType.SUPPORTS)
    public CarWithOfferDTO getCarByIdWithOffers(@CacheKey Long id) {
        log.info("Fetching car with offers by ID: {}", id);
        return carRepository.findByIdOptional(id)
                .map(CarMapper::toDTOWithOffers)
                .orElseThrow(() -> new CarNotFoundException(id));
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = "car-cache")
    public CarDTO createCar(CreateCarDTO createCarDTO) {
        log.info("Creating new car with details: {}", createCarDTO);
        if (carRepository.existsByVin(createCarDTO.vin())) {
            throw new DuplicateCarException("Car with VIN already exists: " + createCarDTO.vin());
        }

        Car car = new Car.Builder()
                    .brand(createCarDTO.brand())
                    .model(createCarDTO.model())
                    .year(createCarDTO.year())
                    .color(createCarDTO.color())
                    .fuelType(createCarDTO.fuelType())
                    .vin(createCarDTO.vin())
                    .build();
            carRepository.persist(car);
            return CarMapper.toDTO(car);
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = "car-cache")
    @CacheInvalidate(cacheName = "car-offers-cache")
    public CarDTO updateCar(@CacheKey Long id, UpdateCarDTO carDTO) {
        log.info("Updating car ID: {}", id);
        Car car = carRepository.findByIdOptional(id)
                    .orElseThrow(() -> new CarNotFoundException(id));
            car.setBrand(carDTO.brand());
            car.setModel(carDTO.model());
            car.setYear(carDTO.year());
            car.setColor(carDTO.color());
            car.setFuelType(carDTO.fuelType());
            return CarMapper.toDTO(car);
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = "car-cache")
    public void deleteCar(@CacheKey Long id) {
        log.info("Deleting car ID: {}", id);
        Car car = carRepository.findByIdOptional(id)
                    .orElseThrow(() -> new EntityNotFoundException("Car with ID " + id + " not found"));
            carRepository.delete(car);
    }

    @Override
    public PaginatedResponse<CarDTO> findByBrandAndModel(String brand, String model, int page, int size) {
        log.info("Searching cars by brand '{}' and model '{}'", brand, model);
        if ((brand == null || brand.isBlank()) && (model == null || model.isBlank())) {
            throw new IllegalArgumentException("Brand or model must be provided.");
        }
        PanacheQuery<Car> carQuery = carRepository.findByBrandAndModelPaged(brand, model, page, size);
        long totalItems = carQuery.count();
        List<CarDTO> carDTOs = carQuery.stream()
                .map(CarMapper::toDTO).toList();

        return new PaginatedResponse<>(carDTOs, totalItems, carQuery.pageCount(), page, size);
    }

    @Override
    public PaginatedResponse<CarDTO> findCarsByYearRange(int startYear, int endYear, int page, int size) {
        log.info("Fetching cars between year range {} and {}", startYear, endYear);

        if (startYear <= 0 || endYear <= 0) {
            throw new IllegalArgumentException("At least one of the years (startYear or endYear) must be a valid positive number.");
        }

        PanacheQuery<Car> carQuery = carRepository.findByYearBetweenPaged(startYear, endYear, page, size);
        long totalItems = carQuery.count();

        List<CarDTO> carDTOs = carQuery
                .stream().map(CarMapper::toDTO).toList();
        return new PaginatedResponse<>(carDTOs, totalItems, carQuery.pageCount(), page, size);
    }

    @Override
    @CacheResult(cacheName = "search-cache")
    public List<CarDTO> searchCars(String brand, String model, Integer year, String color, FuelType fuelType, String sortBy,
                                   boolean asc, int page, int size) {
        log.info("Searching cars with filters: brand={}, model={}, year={}, color={}", brand, model, year, color);
        List<Car> cars = carRepository.searchCar(brand, model, year, color, fuelType, sortBy, asc, page, size);
        return cars.stream()
                .map(CarMapper::toDTO)
                .collect(Collectors.toList());
    }



}
