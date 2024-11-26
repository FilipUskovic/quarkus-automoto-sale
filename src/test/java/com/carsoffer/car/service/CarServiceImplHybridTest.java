package com.carsoffer.car.service;

import com.carsoffer.car.dto.CarDTO;
import com.carsoffer.car.dto.CreateCarDTO;
import com.carsoffer.car.dto.FuelType;
import com.carsoffer.car.dto.UpdateCarDTO;
import com.carsoffer.car.entity.Car;
import com.carsoffer.car.repository.CarRepository;
import com.carsoffer.common.customvalidations.Alphanumeric;
import com.carsoffer.common.customvalidations.AlphanumericValidator;
import com.carsoffer.common.exceptions.CarNotFoundException;
import com.carsoffer.common.exceptions.DuplicateCarException;
import com.carsoffer.common.utils.PaginatedResponse;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class CarServiceImplHybridTest {

    @Inject
    CarServiceImpl carService;

    @InjectMock
    CarRepository carRepository;

    @Inject
    CacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        cacheManager.getCache("car-cache").ifPresent(carCache ->
                carCache.invalidateAll().await().indefinitely());
    }

    @Test
    void testGetCarById_CacheEnabled() {
        Car car = new Car.Builder()
                .id(1L)
                .brand("Toyota")
                .model("Corola")
                .fuelType(FuelType.HYBRID).build();

        when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(car));
        CarDTO carDTO1 = carService.getCarById(1L);
        verify(carRepository, times(1)).findByIdOptional(1L);

        // fetch from cache
        CarDTO carDTO2 = carService.getCarById(1L);
        verify(carRepository, times(1)).findByIdOptional(1L); // Should still be 1
        assertEquals(carDTO1, carDTO2);

    }


    @Test
    void testCreateCar_CacheInvalidation() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("BMW");
        car.setModel("X5");
        car.setVin("12212");

        // ne trebam mockat return za persist() vraca void
        doNothing().when(carRepository).persist(any(Car.class));



        CarDTO createdCar = carService.createCar(new CreateCarDTO("BMW", "X5", 2022, "White", FuelType.PETROL, "12212"));

        assertNotNull(createdCar);
        verify(carRepository, times(1)).persist(any(Car.class));

        Cache carCache = cacheManager.getCache("car-cache").get();
        Object cachedValue = carCache.get(1L, k -> null).await().indefinitely();
        assertNull(cachedValue);

    }


    @Test
    @Transactional
    void testUpdateCar() {
        Car existingCar = new Car();
        existingCar.setId(1L);
        existingCar.setBrand("Ford");
        existingCar.setModel("Mustang");
        existingCar.setColor("White");

        when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingCar));

        UpdateCarDTO updateCarDTO = new UpdateCarDTO("Ford", "Mustang", 2022, "Black", FuelType.DIESEL);
        CarDTO updatedCarDTO = carService.updateCar(1L, updateCarDTO);

        assertEquals("Black", updatedCarDTO.color());
        assertEquals(FuelType.DIESEL, updatedCarDTO.fuelType());

        verify(carRepository, times(1)).findByIdOptional(1L);
        verify(carRepository, never()).persist(existingCar); // persist() may not be needed
        verifyNoMoreInteractions(carRepository);

        assertEquals("Black", existingCar.getColor());
        assertEquals(FuelType.DIESEL, existingCar.getFuelType());
    }

    @Test
    void testGetCarById_NotFound() {
        when(carRepository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(CarNotFoundException.class, () -> carService.getCarById(999L));
    }



    @Test
    void testFindByBrandAndModel() {
        PanacheQuery<Car> query = mock(PanacheQuery.class);

        Car car = new Car();
        car.setId(1L);
        car.setBrand("Audi");
        car.setModel("A4");

        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(car));
        when(query.count()).thenReturn(1L);
        when(query.pageCount()).thenReturn(1);


        when(carRepository.findByBrandAndModelPaged("Audi", "A4", 0, 10)).thenReturn(query);

        PaginatedResponse<CarDTO> cars = carService.findByBrandAndModel("Audi", "A4", 0, 10);
        assertEquals(1, cars.getItems().size());
        assertEquals("Audi", cars.getItems().getFirst().brand());
        assertEquals("A4", cars.getItems().getFirst().model());
        assertEquals(1L, cars.getTotalItems());
        assertEquals(1, cars.getTotalPages());
        assertEquals(0, cars.getCurrentPage());
    }

    @Test
    void testFindByBrandAndModelWithBrandOnly() {
        PanacheQuery<Car> query = mock(PanacheQuery.class);

        Car car = new Car();
        car.setId(1L);
        car.setBrand("Audi");
        car.setModel("A4");

        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(car));
        when(query.count()).thenReturn(1L);
        when(query.pageCount()).thenReturn(1);

        when(carRepository.findByBrandAndModelPaged("Audi", "", 0, 10)).thenReturn(query);

        PaginatedResponse<CarDTO> cars = carService.findByBrandAndModel("Audi", "", 0, 10);
        assertEquals(1, cars.getItems().size());
        assertEquals("Audi", cars.getItems().getFirst().brand());
        assertEquals("A4", cars.getItems().getFirst().model());
        assertEquals(1L, cars.getTotalItems());
        assertEquals(1, cars.getTotalPages());
        assertEquals(0, cars.getCurrentPage());
    }

    @Test
    void testFindByBrandAndModelWithModelOnly() {
        PanacheQuery<Car> query = mock(PanacheQuery.class);

        Car car = new Car();
        car.setId(1L);
        car.setBrand("Audi");
        car.setModel("A4");

        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(car));
        when(query.count()).thenReturn(1L);
        when(query.pageCount()).thenReturn(1);

        when(carRepository.findByBrandAndModelPaged("", "A4", 0, 10)).thenReturn(query);

        PaginatedResponse<CarDTO> cars = carService.findByBrandAndModel("", "A4", 0, 10);
        assertEquals(1, cars.getItems().size());
        assertEquals("Audi", cars.getItems().getFirst().brand());
        assertEquals("A4", cars.getItems().getFirst().model());
        assertEquals(1L, cars.getTotalItems());
        assertEquals(1, cars.getTotalPages());
        assertEquals(0, cars.getCurrentPage());
    }

    @Test
    void testFindByBrandAndModelWithNoFields() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            carService.findByBrandAndModel("", "", 0, 10);
        });

        assertEquals("Brand or model must be provided.", thrown.getMessage());
    }


    @Test
    @Transactional
    void testCreateCarTransactional() {
        CreateCarDTO createCarDTO = new CreateCarDTO("Mercedes", "C-Class", 2021, "Black", FuelType.PETROL, "12212");

        when(carRepository.existsByVin(createCarDTO.vin())).thenReturn(false);

        CarDTO createdCarDTO = carService.createCar(createCarDTO);

        assertNotNull(createdCarDTO);
        assertEquals("Mercedes", createdCarDTO.brand());

        verify(carRepository, times(1)).persist(any(Car.class));

    }

    @Test
    @Transactional
    void testCreateCar_DuplicateVIN() {
        CreateCarDTO createCarDTO = new CreateCarDTO("Mercedes", "C-Class", 2021, "Black", FuelType.PETROL, "12212");

        when(carRepository.existsByVin(createCarDTO.vin())).thenReturn(true);

        assertThrows(DuplicateCarException.class, () -> {
            carService.createCar(createCarDTO);
        });

        verify(carRepository, never()).persist(any(Car.class));
    }

    @Test
    @Transactional
    void testDeleteCar() {
        Car existingCar = new Car();
        existingCar.setId(1L);
        existingCar.setBrand("BMW");
        existingCar.setModel("M3");

        when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingCar));

        carService.deleteCar(1L);

        verify(carRepository, times(1)).findByIdOptional(1L);
        verify(carRepository, times(1)).delete(existingCar);
    }

    @Test
    void testSearchCarsByCriteria() {
        PanacheQuery<Car> query = mock(PanacheQuery.class);

        Car car = new Car();
        car.setId(1L);
        car.setBrand("Mercedes");
        car.setModel("C-Class");

        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(car));

        when(carRepository.searchCar(anyString(), anyString(), any(), anyString(), any(), anyString(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of(car));

        List<CarDTO> cars = carService.searchCars("M", "C-Class", 2021, "Black", FuelType.PETROL, "brand", true, 0, 10);
        assertEquals(1, cars.size());
        assertEquals("Mercedes", cars.getFirst().brand());
        assertEquals("C-Class", cars.getFirst().model());
    }

    @Test
    void testSearchCars_EmptyResult() {
        when(carRepository.searchCar(anyString(), anyString(), any(), anyString(), any(), anyString(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of());

        List<CarDTO> cars = carService.searchCars("NonExistentBrand", "", null, "", null, "", true, 0, 10);
        assertTrue(cars.isEmpty());
    }

    @Test
    @Transactional
    void testUpdateCar_CacheInvalidation() {
        Car existingCar = new Car();
        existingCar.setId(1L);
        existingCar.setBrand("Audi");
        existingCar.setModel("A4");
        when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingCar));
        UpdateCarDTO updateCarDTO = new UpdateCarDTO("Audi", "A4", 2022, "Blue", FuelType.PETROL);
        carService.updateCar(1L, updateCarDTO);
        Cache carCache = cacheManager.getCache("car-cache").get();
        Object cachedValue = carCache.get(1L, k -> null).await().indefinitely();
        assertNull(cachedValue);

        verify(carRepository, times(1)).findByIdOptional(1L);
        verify(carRepository, never()).persist(existingCar);
    }

    @Test
    @Transactional
    void testDeleteCar_CacheInvalidation() {
        Car existingCar = new Car();
        existingCar.setId(1L);
        existingCar.setBrand("BMW");
        existingCar.setModel("M3");
        when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingCar));

        carService.deleteCar(1L);

        Cache carCache = cacheManager.getCache("car-cache").get();
        Object cachedValue = carCache.get(1L, k -> null).await().indefinitely();
        assertNull(cachedValue);

        verify(carRepository, times(1)).findByIdOptional(1L);
        verify(carRepository, times(1)).delete(existingCar);
    }

    @Test
    @Transactional
    void testUpdateCar_NotFound() {
        UpdateCarDTO updateCarDTO = new UpdateCarDTO("Ford", "Mustang", 2022, "Black", FuelType.DIESEL);
        when(carRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        assertThrows(CarNotFoundException.class, () -> carService.updateCar(999L, updateCarDTO));
        verify(carRepository, times(1)).findByIdOptional(999L);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @Transactional
    void testDeleteCar_NotFound() {
        when(carRepository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> carService.deleteCar(999L));
        verify(carRepository, times(1)).findByIdOptional(999L);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    void testAlphanumericValidation() {
        AlphanumericValidator validator = new AlphanumericValidator();
        validator.initialize(new Alphanumeric() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public int min() { return 1; }
            @Override
            public int max() { return 50; }

            @Override
            public boolean lettersOnly() {
                return false;
            }

            @Override
            public boolean allowSpecialCharacters() { return true; }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }
        });

        assertTrue(validator.isValid("Golf 5", null)); // Should pass
        assertFalse(validator.isValid("!Invalid@", null)); // Should fail
    }

}

