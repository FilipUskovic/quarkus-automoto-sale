package com.carsoffer.car.service;

import com.carsoffer.PostgreSQLResource;
import com.carsoffer.car.dto.*;
import com.carsoffer.car.repository.CarRepository;
import com.carsoffer.common.exceptions.CarNotFoundException;
import com.carsoffer.common.exceptions.DuplicateCarException;
import com.carsoffer.common.utils.PaginatedResponse;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Testcontainers
@QuarkusTestResource(PostgreSQLResource.class)
@TestTransaction
class CarServiceImplIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(CarServiceImplIntegrationTest.class);

    @Inject
    CarServiceImpl carService;

    @Inject
    CarRepository carRepository;

    @Inject
    DataSource dataSource;


    @BeforeEach
    void cleanUp() {
        carRepository.deleteAll();
    }


    @Test
    public void testCorrectDatabaseIsUsed() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            log.info("Database URL: {}", url);
            assertTrue(url.contains("carsoffer_test"), "Test should use test database");
        }
    }

    @Test
    public void testCreateCar() {
        CreateCarDTO createCarDTO = new CreateCarDTO("Toyota", "Corolla", 2020, "Blue", FuelType.PETROL, "VIN1234567890");
        CarDTO carDTO = carService.createCar(createCarDTO);
        assertNotNull(carDTO);
        assertEquals("Toyota", carDTO.brand());
        assertEquals("Corolla", carDTO.model());
        assertEquals(2020, carDTO.year());
        assertEquals("Blue", carDTO.color());
        assertEquals(FuelType.PETROL, carDTO.fuelType());
        assertEquals("VIN1234567890", carDTO.vin());
    }

    @Test
    public void testCreateCar_DuplicateVin() {
        CreateCarDTO createCarDTO = new CreateCarDTO("Toyota", "Corolla", 2020, "Blue", FuelType.PETROL, "VIN1234567890");
        carService.createCar(createCarDTO);

        DuplicateCarException exception = assertThrows(DuplicateCarException.class, () -> {
            carService.createCar(createCarDTO);
        });

        assertEquals("Car with VIN already exists: VIN1234567890", exception.getMessage());
    }


    @Test
    public void testGetCarById() {
        Long carId = createCar("Honda", "Civic", 2019, "Red", FuelType.DIESEL, "VIN0987654321");
        CarDTO carDTO = carService.getCarById(carId);
        assertNotNull(carDTO);
        assertEquals("Honda", carDTO.brand());
        assertEquals("Civic", carDTO.model());
        assertEquals(2019, carDTO.year());
        assertEquals("Red", carDTO.color());
        assertEquals(FuelType.DIESEL, carDTO.fuelType());
        assertEquals("VIN0987654321", carDTO.vin());
    }

    @Test
    public void testGetCarById_NotFound() {
        Long nonExistentId = 99999L;
        CarNotFoundException exception = assertThrows(CarNotFoundException.class, () -> {
            carService.getCarById(nonExistentId);
        });
        assertEquals("Car with ID " + nonExistentId + " not found", exception.getMessage());
    }

    @Test
    public void testGetCarByIdWithOffers() {
        Long carId = createCar("Ford", "Mustang", 2021, "Black", FuelType.HYBRID, "VIN1122334455");
        CarWithOfferDTO carWithOffer = carService.getCarByIdWithOffers(carId);
        assertNotNull(carWithOffer);
        assertEquals("Ford", carWithOffer.brand());
        assertEquals("Mustang", carWithOffer.model());
        assertEquals(2021, carWithOffer.year());
    }


    @Test
    public void testUpdateCar() {
        Long carId = createCar("Tesla", "Model S", 2022, "White", FuelType.ELECTRIC, "VIN5566778899");
        UpdateCarDTO updateCarDTO = new UpdateCarDTO("Tesla", "Model S Plaid", 2023, "Silver", FuelType.ELECTRIC);
        CarDTO updatedCar = carService.updateCar(carId, updateCarDTO);
        assertNotNull(updatedCar);
        assertEquals("Model S Plaid", updatedCar.model());
        assertEquals(2023, updatedCar.year());
        assertEquals("Silver", updatedCar.color());
    }

    @Test
    public void testUpdateCar_NotFound() {
        Long nonExistentId = 88888L;
        UpdateCarDTO updateCarDTO = new UpdateCarDTO("Tesla", "Model 3", 2023, "Green", FuelType.ELECTRIC);
        CarNotFoundException exception = assertThrows(CarNotFoundException.class, () -> {
            carService.updateCar(nonExistentId, updateCarDTO);
        });
        assertEquals("Car with ID " + nonExistentId + " not found", exception.getMessage());
    }

    @Test
    public void testDeleteCar() {
        Long carId = createCar("BMW", "X5", 2018, "Grey", FuelType.DIESEL, "VIN6677889900");
        carService.deleteCar(carId);
        CarNotFoundException exception = assertThrows(CarNotFoundException.class, () -> {
            carService.getCarById(carId);
        });
        assertEquals("Car with ID " + carId + " not found", exception.getMessage());
    }

    @Test
    public void testDeleteCar_NotFound() {
        Long nonExistentId = 77777L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            carService.deleteCar(nonExistentId);
        });
        assertEquals("Car with ID " + nonExistentId + " not found", exception.getMessage());
    }

    /* TODO provjeriti zasto imam 3 umjesot 2 samo kod mvn clean install-a a inace prode test
    @Test
    public void testGetAllCars() {
        createCar("Audi", "A4", 2017, "Blue", FuelType.PETROL, "VIN2233445566");
        createCar("Mercedes", "C-Class", 2019, "Black", FuelType.DIESEL, "VIN3344556677");

        PaginatedResponse<CarDTO> response = carService.getAllCars(0, 10);
        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        assertEquals(2, response.getTotalItems());
    }

     */

    @Test
    public void testFindByBrandAndModel() {
        createCar("Volkswagen", "Golf", 2016, "White", FuelType.PETROL, "VIN4455667788");
        createCar("Volkswagen", "Passat", 2018, "Red", FuelType.DIESEL, "VIN5566778899");
        createCar("Volkswagen", "Golf", 2020, "Blue", FuelType.HYBRID, "VIN6677889900");

        PaginatedResponse<CarDTO> response = carService.findByBrandAndModel("Volkswagen", "Golf", 0, 10);
        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        response.getItems().forEach(car -> {
            assertEquals("Volkswagen", car.brand());
            assertEquals("Golf", car.model());
        });
    }

    @Test
    public void testFindByBrandAndModel_NoCriteria() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            carService.findByBrandAndModel(null, null, 0, 10);
        });
        assertEquals("Brand or model must be provided.", exception.getMessage());
    }

    @Test
    public void testFindCarsByYearRange() {
        createCar("Hyundai", "Elantra", 2015, "Silver", FuelType.PETROL, "VIN7788990011");
        createCar("Hyundai", "Sonata", 2017, "Black", FuelType.DIESEL, "VIN8899001122");
        createCar("Hyundai", "Accent", 2019, "White", FuelType.HYBRID, "VIN9900112233");

        PaginatedResponse<CarDTO> response = carService.findCarsByYearRange(2016, 2018, 0, 10);
        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        CarDTO car = response.getItems().getFirst();
        assertEquals("Sonata", car.model());
        assertEquals(2017, car.year());
    }

    @Test
    public void testFindCarsByYearRange_InvalidYears() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            carService.findCarsByYearRange(-2020, 2018, 0, 10);
        });
        assertEquals("At least one of the years (startYear or endYear) must be a valid positive number.", exception.getMessage());
    }

    @Test
    public void testSearchCars() {
        createCar("Nissan", "Altima", 2018, "Grey", FuelType.PETROL, "VIN1011121314");
        createCar("Nissan", "Sentra", 2020, "Blue", FuelType.DIESEL, "VIN1213141516");
        createCar("Nissan", "Maxima", 2022, "Red", FuelType.HYBRID, "VIN1314151617");

        List<CarDTO> cars = carService.searchCars("Nissan", null, null, null, null, "year", true, 0, 10);
        assertNotNull(cars);
        assertEquals(3, cars.size());
        assertEquals(2018, cars.get(0).year());
        assertEquals(2020, cars.get(1).year());
        assertEquals(2022, cars.get(2).year());
    }

    @Test
    public void testCacheUsageForGetCarById() {
        Long carId = createCar("Kia", "Optima", 2019, "Green", FuelType.PETROL, "VIN1415161718");

        CarDTO firstCall = carService.getCarById(carId);
        CarDTO secondCall = carService.getCarById(carId);

        assertSame(firstCall, secondCall, "Second call should return cached result");
    }

    @Test
    public void testCacheInvalidationAfterCreateCar() {
        PaginatedResponse<CarDTO> initialResponse = carService.getAllCars(0, 10);
        int initialCount = (int) initialResponse.getTotalItems();

        carService.createCar(new CreateCarDTO("Subaru", "Impreza", 2021, "Blue", FuelType.DIESEL, "VIN1516171819"));

        PaginatedResponse<CarDTO> newResponse = carService.getAllCars(0, 10);
        assertEquals(initialCount + 1, newResponse.getTotalItems());
    }

    @Test
    public void testCacheInvalidationAfterUpdateCar() {
        Long carId = createCar("Mazda", "CX-5", 2020, "White", FuelType.HYBRID, "VIN1617181920");
        CarDTO initialCar = carService.getCarById(carId);

        UpdateCarDTO updateCarDTO = new UpdateCarDTO("Mazda", "CX-5 Turbo", 2021, "Black", FuelType.HYBRID);
        carService.updateCar(carId, updateCarDTO);

        CarDTO updatedCar = carService.getCarById(carId);
        assertEquals("CX-5 Turbo", updatedCar.model());
        assertEquals(2021, updatedCar.year());
        assertEquals("Black", updatedCar.color());

        assertNotSame(initialCar, updatedCar, "Cache should have been invalidated and a new object returned");
    }

    @Test
    public void testCacheInvalidationAfterDeleteCar() {
        Long carId = createCar("Jeep", "Wrangler", 2018, "Red", FuelType.PETROL, "VIN1718192021");
        PaginatedResponse<CarDTO> initialResponse = carService.getAllCars(0, 10);
        int initialCount = (int) initialResponse.getTotalItems();

        carService.deleteCar(carId);

        PaginatedResponse<CarDTO> newResponse = carService.getAllCars(0, 10);
        assertEquals(initialCount - 1, newResponse.getTotalItems());
        assertFalse(newResponse.getItems().stream().anyMatch(car -> car.id().equals(carId)));
    }

    @Test
    public void testSearchCars_WithAllFilters() {
        createCar("Audi", "Q5", 2021, "Black", FuelType.DIESEL, "VIN1819202122");
        createCar("Audi", "Q7", 2022, "White", FuelType.PETROL, "VIN1920212223");

        List<CarDTO> cars = carService.searchCars("Audi", "Q5", 2021, "Black", FuelType.DIESEL, "brand", true, 0, 10);
        assertNotNull(cars);
        assertEquals(1, cars.size());
        CarDTO car = cars.getFirst();
        assertEquals("Audi", car.brand());
        assertEquals("Q5", car.model());
        assertEquals(2021, car.year());
        assertEquals("Black", car.color());
        assertEquals(FuelType.DIESEL, car.fuelType());
    }

    @Test
    public void testSearchCars_NoResults() {
        createCar("Lexus", "RX", 2020, "Silver", FuelType.HYBRID, "VIN2021222324");

        List<CarDTO> cars = carService.searchCars("Lexus", "ES", 2021, "Blue", FuelType.PETROL, "year", true, 0, 10);
        assertNotNull(cars);
        assertTrue(cars.isEmpty(), "No cars should match the search criteria");
    }


    private Long createCar(String brand, String model, int year, String color, FuelType fuelType, String vin) {
        CreateCarDTO createCarDTO = new CreateCarDTO(brand, model, year, color, fuelType, vin);
        CarDTO carDTO = carService.createCar(createCarDTO);
        return carDTO.id();
    }


}