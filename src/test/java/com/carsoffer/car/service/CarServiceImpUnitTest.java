package com.carsoffer.car.service;

import com.carsoffer.car.dto.CarDTO;
import com.carsoffer.car.dto.CreateCarDTO;
import com.carsoffer.car.dto.FuelType;
import com.carsoffer.car.dto.UpdateCarDTO;
import com.carsoffer.car.entity.Car;
import com.carsoffer.car.repository.CarRepository;
import com.carsoffer.common.exceptions.CarNotFoundException;
import com.carsoffer.common.exceptions.DuplicateCarException;
import com.carsoffer.common.utils.PaginatedResponse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CarServiceImpUnitTest {

    @Mock
    CarRepository carRepository;

    @InjectMocks
    CarServiceImpl carService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateCar_DuplicateVIN() {
        CreateCarDTO createCarDTO = new CreateCarDTO("Mercedes", "C-Class", 2021, "Black", FuelType.PETROL, "12212");

        when(carRepository.existsByVin(createCarDTO.vin())).thenReturn(true);

        DuplicateCarException thrown = assertThrows(DuplicateCarException.class, () -> {
            carService.createCar(createCarDTO);
        });

        assertEquals("Car with VIN already exists: 12212", thrown.getMessage());
        verify(carRepository, never()).persist(any(Car.class));
    }


    @Test
    void testCreateCar_Success() {
        CreateCarDTO createCarDTO = new CreateCarDTO("BMW", "X5", 2022, "White", FuelType.PETROL, "12212");
        Car car = new Car();
        car.setId(1L);

        when(carRepository.existsByVin(createCarDTO.vin())).thenReturn(false);
        doNothing().when(carRepository).persist(any(Car.class));

        CarDTO createdCar = carService.createCar(createCarDTO);

        assertNotNull(createdCar);
        verify(carRepository, times(1)).persist(any(Car.class));
    }

    @Test
    void testUpdateCar_Success() {
        Car existingCar = new Car();
        existingCar.setId(1L);
        existingCar.setBrand("Ford");
        existingCar.setModel("Mustang");

        UpdateCarDTO updateCarDTO = new UpdateCarDTO("Ford", "Mustang", 2022, "Black", FuelType.DIESEL);

        when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingCar));

        CarDTO updatedCar = carService.updateCar(1L, updateCarDTO);

        assertEquals("Black", updatedCar.color());
        verify(carRepository, times(1)).findByIdOptional(1L);
    }

    @Test
    void testUpdateCar_NotFound() {
        UpdateCarDTO updateCarDTO = new UpdateCarDTO("Ford", "Mustang", 2022, "Black", FuelType.DIESEL);

        when(carRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        CarNotFoundException thrown = assertThrows(CarNotFoundException.class, () -> {
            carService.updateCar(999L, updateCarDTO);
        });

        assertEquals("Car with ID 999 not found", thrown.getMessage());
        verify(carRepository, times(1)).findByIdOptional(999L);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    void testDeleteCar_Success() {
        Car existingCar = new Car();
        existingCar.setId(1L);
        existingCar.setBrand("BMW");

        when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingCar));

        carService.deleteCar(1L);

        verify(carRepository, times(1)).findByIdOptional(1L);
        verify(carRepository, times(1)).delete(existingCar);
    }

    @Test
    void testDeleteCar_NotFound() {
        when(carRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () -> {
            carService.deleteCar(999L);
        });

        assertEquals("Car with ID 999 not found", thrown.getMessage());
        verify(carRepository, times(1)).findByIdOptional(999L);
    }


    @Test
    void testGetCarById_Success() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("Toyota");

        when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(car));

        CarDTO foundCar = carService.getCarById(1L);

        assertNotNull(foundCar);
        assertEquals("Toyota", foundCar.brand());
        verify(carRepository, times(1)).findByIdOptional(1L);
    }

    @Test
    void testGetCarById_NotFound() {
        when(carRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        CarNotFoundException thrown = assertThrows(CarNotFoundException.class, () -> {
            carService.getCarById(999L);
        });

        assertEquals("Car with ID 999 not found", thrown.getMessage());
        verify(carRepository, times(1)).findByIdOptional(999L);
    }

    @Test
    void testSearchCars_Success() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("Mercedes");

        PanacheQuery<Car> query = mock(PanacheQuery.class);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(car));

        when(carRepository.searchCar(anyString(), anyString(), any(), anyString(), any(), anyString(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of(car));

        List<CarDTO> cars = carService.searchCars("M", "C-Class", 2021, "Black", FuelType.PETROL, "brand", true, 0, 10);

        assertEquals(1, cars.size());
        assertEquals("Mercedes", cars.getFirst().brand());
    }

    @Test
    void testSearchCars_NoResult() {
        when(carRepository.searchCar(anyString(), anyString(), any(), anyString(), any(), anyString(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of());

        List<CarDTO> cars = carService.searchCars("NonExistentBrand", "", null, "", null, "", true, 0, 10);

        assertTrue(cars.isEmpty());
    }

    @Test
    void testFindByBrandAndModel_Success() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("Audi");
        car.setModel("A4");

        PanacheQuery<Car> query = mock(PanacheQuery.class);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(car));
        when(query.count()).thenReturn(1L);

        when(carRepository.findByBrandAndModelPaged("Audi", "A4", 0, 10)).thenReturn(query);

        PaginatedResponse<CarDTO> cars = carService.findByBrandAndModel("Audi", "A4", 0, 10);

        assertEquals(1, cars.getItems().size());
        assertEquals("Audi", cars.getItems().getFirst().brand());
        assertEquals("A4", cars.getItems().getFirst().model());
        assertEquals(1L, cars.getTotalItems());
        assertEquals(0, cars.getCurrentPage());

    }

    @Test
    void testFindByBrandAndModel_NoResults() {
        PanacheQuery<Car> query = mock(PanacheQuery.class);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.empty());
        when(query.count()).thenReturn(0L);

        when(carRepository.findByBrandAndModelPaged("Audi", "A4", 0, 10)).thenReturn(query);

        PaginatedResponse<CarDTO> cars = carService.findByBrandAndModel("Audi", "A4", 0, 10);

        assertTrue(cars.getItems().isEmpty());
        assertEquals(0, cars.getTotalItems());
        assertEquals(0, cars.getTotalPages());
    }

    @Test
    void testFindByBrandAndModel_InvalidParameters() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            carService.findByBrandAndModel("", "", 0, 10);
        });
        assertEquals("Brand or model must be provided.", exception.getMessage());
    }


    @Test
    void testSearchCarsByFuelType() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("Toyota");
        car.setModel("Prius");
        car.setFuelType(FuelType.HYBRID);

        PanacheQuery<Car> query = mock(PanacheQuery.class);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(car));

        when(carRepository.searchCar(anyString(), anyString(), anyInt(), anyString(), eq(FuelType.HYBRID), anyString(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of(car));

        List<CarDTO> cars = carService.searchCars("T", "Prius", 2021, "Green", FuelType.HYBRID, "brand", true, 0, 10);

        assertEquals(1, cars.size());
        assertEquals("Toyota", cars.getFirst().brand());
        assertEquals(FuelType.HYBRID, cars.getFirst().fuelType());
    }


    @Test
    void testSearchCars_NoResultsByFuelType() {
        when(carRepository.searchCar(anyString(), anyString(), anyInt(), anyString(), eq(FuelType.DIESEL), anyString(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of());
        List<CarDTO> cars = carService.searchCars("", "", null, "", FuelType.DIESEL, "", true, 0, 10);
        assertTrue(cars.isEmpty());
    }



}

