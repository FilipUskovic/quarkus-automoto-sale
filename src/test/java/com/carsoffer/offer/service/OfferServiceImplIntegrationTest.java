package com.carsoffer.offer.service;

import com.carsoffer.PostgreSQLResource;
import com.carsoffer.car.dto.CarDTO;
import com.carsoffer.car.dto.FuelType;
import com.carsoffer.car.entity.Car;
import com.carsoffer.car.repository.CarRepository;
import com.carsoffer.car.service.CarService;
import com.carsoffer.common.exceptions.CarNotFoundException;
import com.carsoffer.common.exceptions.OfferNotFoundException;
import com.carsoffer.common.utils.PaginatedResponse;
import com.carsoffer.offer.dto.CreateOfferDTO;
import com.carsoffer.offer.dto.OfferDTO;
import com.carsoffer.offer.dto.OfferSearchCriteria;
import com.carsoffer.offer.dto.UpdateOfferDTO;
import com.carsoffer.offer.entity.Offer;
import com.carsoffer.offer.repository.OfferRepository;
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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Testcontainers
@QuarkusTestResource(PostgreSQLResource.class)
@TestTransaction
class OfferServiceImplIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(OfferServiceImplIntegrationTest.class);

    @Inject
    OfferService offerService;

    @Inject
    CarService carService;

    @Inject
    CarRepository carRepository;

    @Inject
    OfferRepository offerRepository;

    @Inject
    DataSource dataSource;


    @BeforeEach
    void cleanUp() {
        offerRepository.deleteAll();
        carRepository.deleteAll();
    }



    @Test
    public void testCorrectDatabaseIsUsed() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            System.out.println("Database URL: " + url);
            assertTrue(url.contains("carsoffer_test"), "Test should use test database");
        }
    }

    @Test
    public void testCreateOffer() {
        Long carId = createCar();
        CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(15000), carId);
        OfferDTO offerDTO = offerService.createOffer(createOfferDTO);
        assertNotNull(offerDTO);
        assertEquals("Luka", offerDTO.customerFirstName());
        assertEquals("Borna", offerDTO.customerLastName());
        assertEquals(BigDecimal.valueOf(15000), offerDTO.price());
    }

    @Test
    public void testFindOfferById() {
        Long carId = createCar();
        Offer offer = new Offer.Builder()
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(15000.00))
                .offerDate(LocalDateTime.now())
                .car(new Car.Builder().id(carId).build())
                .build();

        CreateOfferDTO createOfferDTO = new CreateOfferDTO(
                offer.getCustomerFirstName(),
                offer.getCustomerLastName(),
                offer.getPrice(),
                carId
        );

        OfferDTO createdOffer = offerService.createOffer(createOfferDTO);
        assertNotNull(createdOffer);
        assertEquals("Luka", createdOffer.customerFirstName());

        OfferDTO foundOffer = offerService.findOfferById(createdOffer.id());
        assertNotNull(foundOffer);
        assertEquals("Luka", foundOffer.customerFirstName());
        assertEquals(0, foundOffer.price().compareTo(BigDecimal.valueOf(15000.00)));

        OfferDTO offerDTO = offerService.findOfferById(createdOffer.id());
        assertNotNull(offerDTO);
        assertEquals(createdOffer.id(), offerDTO.id());
    }

    @Test
    public void testUpdateOffer() {
        Long carId = createCar();

        CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(15000), carId);
        OfferDTO offerDTO = offerService.createOffer(createOfferDTO);

        OfferDTO updatedOfferDTO = offerService.updateOffer(offerDTO.id(), new UpdateOfferDTO("Luka", "Smith", BigDecimal.valueOf(18000), carId));

        assertNotNull(updatedOfferDTO);
        assertEquals("Luka", updatedOfferDTO.customerFirstName());
        assertEquals(BigDecimal.valueOf(18000), updatedOfferDTO.price());
    }

    @Test
    public void testDeleteOffer() {
        Long carId = createCar();
        CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(15000), carId);
        OfferDTO offerDTO = offerService.createOffer(createOfferDTO);
        offerService.deleteOffer(offerDTO.id());
        assertThrows(OfferNotFoundException.class, () -> offerService.findOfferById(offerDTO.id()));
    }

    @Test
    public void testCreateOfferWithInvalidCar() {
        CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(15000), 999L);
        assertThrows(CarNotFoundException.class, () -> offerService.createOffer(createOfferDTO));
    }



    @Test
    public void testPaginatedOffers() {
        Long carId = createCar();
        for (int i = 0; i < 5; i++) {
            CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(15000 + i * 1000), carId);
            offerService.createOffer(createOfferDTO);
        }
        PaginatedResponse<OfferDTO> offersPage1 =  offerService.getAllOffer(0, 2);
        PaginatedResponse<OfferDTO> offersPage2 =  offerService.getAllOffer(1, 2);
        assertEquals(2, offersPage1.getPageSize());
        assertEquals(2, offersPage2.getPageSize());
    }


    @Test
    public void testCacheUsageForGetCarById() {
        Long carId = createCar();
        CarDTO firstCall = carService.getCarById(carId);
        CarDTO secondCall = carService.getCarById(carId);

        assertSame(firstCall, secondCall);
    }





    @Test
    public void testGetOffersByCustomerName_BothNamesProvided() {
        Long carId = createCar();

        offerService.createOffer(new CreateOfferDTO("Alice", "Smith", BigDecimal.valueOf(10000), carId));
        offerService.createOffer(new CreateOfferDTO("Bob", "Jones", BigDecimal.valueOf(12000), carId));
        offerService.createOffer(new CreateOfferDTO("Alice", "Johnson", BigDecimal.valueOf(9000), carId));

        PaginatedResponse<OfferDTO> response = offerService.getOffersByCustomerName("Alice", "Smith", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        OfferDTO offer = response.getItems().getFirst();
        assertEquals("Alice", offer.customerFirstName());
        assertEquals("Smith", offer.customerLastName());
    }

    @Test
    public void testGetOffersByCustomerName_FirstNameOnly() {
        Long carId = createCar();

        offerService.createOffer(new CreateOfferDTO("Alice", "Smith", BigDecimal.valueOf(10000), carId));
        offerService.createOffer(new CreateOfferDTO("Alice", "Johnson", BigDecimal.valueOf(9000), carId));
        offerService.createOffer(new CreateOfferDTO("Bob", "Smith", BigDecimal.valueOf(12000), carId));

        PaginatedResponse<OfferDTO> response = offerService.getOffersByCustomerName("Alice", null, 0, 10);

        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        response.getItems().forEach(offer -> assertEquals("Alice", offer.customerFirstName()));
    }

    @Test
    public void testGetOffersByCustomerName_NoNamesProvided() {
        Long carId = createCar();
        offerService.createOffer(new CreateOfferDTO("Alice", "Smith", BigDecimal.valueOf(10000), carId));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            offerService.getOffersByCustomerName(null, null, 0, 10);
        });

        assertEquals("firstName or lastName must be provided.", exception.getMessage());
    }

    @Test
    public void testGetOffersByPriceRange_ValidPrices() {
        Long carId = createCar();

        offerService.createOffer(new CreateOfferDTO("Alice", "Smith", BigDecimal.valueOf(10000), carId));
        offerService.createOffer(new CreateOfferDTO("Bob", "Jones", BigDecimal.valueOf(15000), carId));
        offerService.createOffer(new CreateOfferDTO("Charlie", "Johnson", BigDecimal.valueOf(20000), carId));

        PaginatedResponse<OfferDTO> response = offerService.getOffersByPriceRange(
                BigDecimal.valueOf(12000), BigDecimal.valueOf(18000), 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        OfferDTO offer = response.getItems().getFirst();
        assertEquals("Bob", offer.customerFirstName());
        assertEquals(0, offer.price().compareTo(BigDecimal.valueOf(15000)));
    }

    @Test
    public void testGetOffersByPriceRange_MinPriceGreaterThanMaxPrice() {
        Long carId = createCar();

        offerService.createOffer(new CreateOfferDTO("Alice", "Smith", BigDecimal.valueOf(10000), carId));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            offerService.getOffersByPriceRange(BigDecimal.valueOf(20000), BigDecimal.valueOf(10000), 0, 10);
        });

        assertEquals("minPrice must be less than or equal to maxPrice.", exception.getMessage());
    }


    @Test
    public void testGetOffersByPriceRange_NullPrices() {
        Long carId = createCar();

        offerService.createOffer(new CreateOfferDTO("Alice", "Smith", BigDecimal.valueOf(10000), carId));

        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            offerService.getOffersByPriceRange(null, BigDecimal.valueOf(10000), 0, 10);
        });
        assertEquals("Prices cannot be null.", exception1.getMessage());

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            offerService.getOffersByPriceRange(BigDecimal.valueOf(10000), null, 0, 10);
        });
        assertEquals("Prices cannot be null.", exception2.getMessage());
    }


    @Test
    public void testSearchOffers_ValidCriteria() {
        Long carId = createCar();

        offerService.createOffer(new CreateOfferDTO("Alice", "Smith", BigDecimal.valueOf(10000), carId));
        offerService.createOffer(new CreateOfferDTO("Bob", "Smith", BigDecimal.valueOf(15000), carId));
        offerService.createOffer(new CreateOfferDTO("Alice", "Johnson", BigDecimal.valueOf(20000), carId));

        OfferSearchCriteria criteria = new OfferSearchCriteria(
                "Alice", null, null, null, null, null, null, true, 0, 10);

        List<OfferDTO> offers = offerService.searchOffers(criteria);

        assertNotNull(offers);
        assertEquals(2, offers.size());
        offers.forEach(offer -> assertEquals("Alice", offer.customerFirstName()));
    }


    @Test
    public void testSearchOffers_NoCriteriaProvided() {
        OfferSearchCriteria criteria = new OfferSearchCriteria(
                null, null, null, null, null, null, null, true, 0, 10);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            offerService.searchOffers(criteria);
        });

        assertEquals("At least one search parameter must be provided.", exception.getMessage());
    }

    @Test
    public void testCacheInvalidationAfterCreateOffer() {
        Long carId = createCar();

        PaginatedResponse<OfferDTO> initialResponse = offerService.getAllOffer(0, 10);

        offerService.createOffer(new CreateOfferDTO("Alice", "Smith", BigDecimal.valueOf(10000), carId));
        PaginatedResponse<OfferDTO> newResponse = offerService.getAllOffer(0, 10);

        assertEquals(initialResponse.getTotalItems() + 1, newResponse.getTotalItems());
    }

    @Test
    public void testCacheInvalidationAfterUpdateOffer() {
        Long carId = createCar();

        OfferDTO offer = offerService.createOffer(new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(10000), carId));
        PaginatedResponse<OfferDTO> initialResponse = offerService.getAllOffer(0, 10);

        offerService.updateOffer(offer.id(), new UpdateOfferDTO("Luka", "Borna", BigDecimal.valueOf(12000), carId));

        PaginatedResponse<OfferDTO> newResponse = offerService.getAllOffer(0, 10);

        OfferDTO updatedOffer = newResponse.getItems().stream()
                .filter(o -> o.id().equals(offer.id()))
                .findFirst()
                .orElse(null);

        assertNotNull(updatedOffer, "Ažurirana ponuda ne smije biti null");
        assertEquals("Luka", updatedOffer.customerFirstName(), "Ime kupca trebalo bi biti ažurirano na 'Luka'");
        assertEquals(0, updatedOffer.price().compareTo(BigDecimal.valueOf(12000)), "Cijena ponude trebala bi biti ažurirana na 12000");
    }


    @Test
    public void testCacheInvalidationAfterDeleteOffer() {
        Long carId = createCar();

        OfferDTO offer = offerService.createOffer(new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(10000), carId));
        PaginatedResponse<OfferDTO> initialResponse = offerService.getAllOffer(0, 10);

        offerService.deleteOffer(offer.id());

        PaginatedResponse<OfferDTO> newResponse = offerService.getAllOffer(0, 10);

        assertEquals(initialResponse.getTotalItems() - 1, newResponse.getTotalItems());
        assertFalse(newResponse.getItems().stream().anyMatch(o -> o.id().equals(offer.id())));
    }




    @Test
    public void testFindOfferById_NotFound() {
        Long nonExistentId = 99999L;

        OfferNotFoundException exception = assertThrows(OfferNotFoundException.class, () -> {
            offerService.findOfferById(nonExistentId);
        });

        assertEquals("offer with ID " + nonExistentId + " not found", exception.getMessage());
    }

    @Test
    public void testDeleteOffer_NotFound() {
        Long nonExistentId = 99999L;

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            offerService.deleteOffer(nonExistentId);
        });

        assertEquals("Offer with ID " + nonExistentId + " not found", exception.getMessage());
    }


    @Test
    public void testUpdateOffer_NotFound() {
        Long nonExistentId = 99999L;
        Long carId = createCar();

        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Luka", "Borna", BigDecimal.valueOf(12000), carId);

        OfferNotFoundException exception = assertThrows(OfferNotFoundException.class, () -> {
            offerService.updateOffer(nonExistentId, updateOfferDTO);
        });

        assertEquals("offer with ID " + nonExistentId + " not found", exception.getMessage());
    }

    @Test
    public void testUpdateOffer_CarNotFound() {
        Long carId = createCar();

        OfferDTO offer = offerService.createOffer(new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(10000), carId));

        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Luka", "Borna", BigDecimal.valueOf(12000), 99999L);

        CarNotFoundException exception = assertThrows(CarNotFoundException.class, () -> {
            offerService.updateOffer(offer.id(), updateOfferDTO);
        });

        assertEquals("Car with ID 99999 not found", exception.getMessage());
    }




    private Long createCar() {
        Car car = new Car.Builder()
                .brand("BMW")
                .model("M5")
                .color("Red")
                .year(2006)
                .fuelType(FuelType.HYBRID)
                .vin("UNKNOWN_13123qwe1")
                .build();

        carRepository.persist(car);
        return car.getId();
    }

}
