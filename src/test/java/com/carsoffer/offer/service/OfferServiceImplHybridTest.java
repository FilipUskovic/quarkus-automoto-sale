package com.carsoffer.offer.service;

import com.carsoffer.car.entity.Car;
import com.carsoffer.car.repository.CarRepository;
import com.carsoffer.common.exceptions.CarNotFoundException;
import com.carsoffer.common.exceptions.OfferNotFoundException;
import com.carsoffer.common.utils.PaginatedResponse;
import com.carsoffer.offer.dto.CreateOfferDTO;
import com.carsoffer.offer.dto.OfferDTO;
import com.carsoffer.offer.dto.OfferSearchCriteria;
import com.carsoffer.offer.dto.UpdateOfferDTO;
import com.carsoffer.offer.entity.Offer;
import com.carsoffer.offer.repository.OfferRepository;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class OfferServiceImplHybridTest {

    @Inject
    OfferServiceImpl offerService;

    @InjectMock
    OfferRepository offerRepository;

    @InjectMock
    CarRepository carRepository;

    @Inject
    CacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        cacheManager.getCache("offer-cache").ifPresent(carCache ->
                carCache.invalidateAll().await().indefinitely());
    }

    @Test
    void testGetOfferById_CacheEnabled() {
        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .offerDate(LocalDateTime.now())
                .build();

        when(offerRepository.findByIdOptional(1L)).thenReturn(Optional.of(offer));
        OfferDTO offerDTO1 = offerService.findOfferById(1L);
        verify(offerRepository, times(1)).findByIdOptional(1L);

        OfferDTO offerDTO2 = offerService.findOfferById(1L);
        verify(offerRepository, times(1)).findByIdOptional(1L);
        assertEquals(offerDTO1, offerDTO2);
    }

    @Test
    @Transactional
    void testCreateOffer_CacheInvalidation() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("BMW");

        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .car(car)
                .offerDate(LocalDateTime.now())
                .build();

        when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(car));
        doNothing().when(offerRepository).persist(any(Offer.class));

        CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(10000), 1L);
        OfferDTO createdOffer = offerService.createOffer(createOfferDTO);

        assertNotNull(createdOffer);
        verify(offerRepository, times(1)).persist(any(Offer.class));

        Cache offerCache = cacheManager.getCache("offer-cache").get();
        Object cachedValue = offerCache.get(1L, k -> null).await().indefinitely();
        assertNull(cachedValue);
    }


    @Test
    @Transactional
    void testUpdateOffer() {
        Car car = new Car();
        car.setId(1L);
        car.setBrand("BMW");

        Offer existingOffer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .car(car)
                .offerDate(LocalDateTime.now())
                .build();

        when(offerRepository.findOfferWithCarById(1L)).thenReturn(Optional.of(existingOffer));

        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Luka", "Borna", BigDecimal.valueOf(20000), 1L);
        OfferDTO updatedOfferDTO = offerService.updateOffer(1L, updateOfferDTO);

        assertEquals("Luka", updatedOfferDTO.customerFirstName());
        assertEquals(BigDecimal.valueOf(20000), updatedOfferDTO.price());

        verify(offerRepository, times(1)).findOfferWithCarById(1L);
        verifyNoMoreInteractions(offerRepository);
    }


    @Test
    @Transactional
    void testUpdateOffer_NotFound() {
        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Luka", "Borna", BigDecimal.valueOf(20000), 1L);

        when(offerRepository.findOfferWithCarById(999L)).thenReturn(Optional.empty());

        OfferNotFoundException thrown = assertThrows(OfferNotFoundException.class, () -> {
            offerService.updateOffer(999L, updateOfferDTO);
        });
        assertEquals("offer with ID 999 not found", thrown.getMessage());

        verify(offerRepository, times(1)).findOfferWithCarById(999L);
        verifyNoMoreInteractions(offerRepository);
    }

    @Test
    void testGetOfferById_NotFound() {
        when(offerRepository.findByIdOptional(999L)).thenReturn(Optional.empty());
        assertThrows(OfferNotFoundException.class, () -> offerService.findOfferById(999L));
    }

    @Test
    void testSearchOffers() {
        OfferSearchCriteria criteria = new OfferSearchCriteria("Luka", "Borna", 5000.0, 15000.0, null, null, "id", true, 0, 10);
        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(offerRepository.searchOffers(criteria)).thenReturn(List.of(offer));

        List<OfferDTO> offers = offerService.searchOffers(criteria);
        assertEquals(1, offers.size());
        assertEquals("Luka", offers.getFirst().customerFirstName());
    }

    @Test
    @Transactional
    void testDeleteOffer() {
        Offer existingOffer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(offerRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingOffer));

        offerService.deleteOffer(1L);

        verify(offerRepository, times(1)).findByIdOptional(1L);
        verify(offerRepository, times(1)).delete(existingOffer);

        Cache offerCache = cacheManager.getCache("offer-cache").get();
        Object cachedValue = offerCache.get(1L, k -> null).await().indefinitely();
        assertNull(cachedValue);
    }

    @Test
    @Transactional
    void testDeleteOffer_NotFound() {
        when(offerRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () -> {
            offerService.deleteOffer(999L);
        });
        assertEquals("Offer with ID 999 not found", thrown.getMessage());

        verify(offerRepository, times(1)).findByIdOptional(999L);
        verify(offerRepository, never()).delete(any(Offer.class));
    }

    @Test
    void testGetOffersByCustomerName() {
        PanacheQuery<Offer> query = mock(PanacheQuery.class);

        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(offer));
        when(query.count()).thenReturn(1L);
        when(query.pageCount()).thenReturn(1);

        when(offerRepository.findByCustomerByFirstNameAndCustomerByLastNamePaged("Luka", "Borna", 0, 10)).thenReturn(query);

        PaginatedResponse<OfferDTO> response = offerService.getOffersByCustomerName("Luka", "Borna", 0, 10);

        assertEquals(1, response.getItems().size());
        assertEquals("Luka", response.getItems().getFirst().customerFirstName());
        assertEquals("Borna", response.getItems().getFirst().customerLastName());
        assertEquals(1, response.getTotalItems());
    }

    @Test
    void testGetOffersByPriceRange() {
        PanacheQuery<Offer> query = mock(PanacheQuery.class);

        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(offer));
        when(query.count()).thenReturn(1L);
        when(query.pageCount()).thenReturn(1);

        when(offerRepository.findByPriceBetweenPaged(BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), 0, 10)).thenReturn(query);

        PaginatedResponse<OfferDTO> response = offerService.getOffersByPriceRange(BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), 0, 10);

        assertEquals(1, response.getItems().size());
        assertEquals(BigDecimal.valueOf(10000), response.getItems().getFirst().price());
        assertEquals(1, response.getTotalItems());
    }


    @Test
    void testValidatePrices() {
        assertDoesNotThrow(() -> offerService.validatePrices(BigDecimal.valueOf(100), BigDecimal.valueOf(200)));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            offerService.validatePrices(BigDecimal.valueOf(-100), BigDecimal.valueOf(200));
        });
        assertEquals("Both minPrice and maxPrice must be positive numbers.", thrown.getMessage());

        thrown = assertThrows(IllegalArgumentException.class, () -> {
            offerService.validatePrices(BigDecimal.valueOf(300), BigDecimal.valueOf(200));
        });
        assertEquals("minPrice must be less than or equal to maxPrice.", thrown.getMessage());
    }

    @Test
    @Transactional
    void testCreateOffer_CarNotFound() {
        CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(10000), 999L);

        when(carRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        CarNotFoundException thrown = assertThrows(CarNotFoundException.class, () -> {
            offerService.createOffer(createOfferDTO);
        });
        assertEquals("Car with ID 999 not found", thrown.getMessage());

        verify(carRepository, times(1)).findByIdOptional(999L);
        verifyNoMoreInteractions(offerRepository);
    }


    @Test
    void testSearchOffers_NoResults() {
        OfferSearchCriteria criteria = new OfferSearchCriteria("NonExistent", "Name", 100.00,
                200.00, null, null, "id", true, 0, 10);
        when(offerRepository.searchOffers(criteria)).thenReturn(List.of());

        List<OfferDTO> offers = offerService.searchOffers(criteria);
        assertTrue(offers.isEmpty());
    }


    @Test
    void testGetAllOffer_CacheEnabled() {
        PanacheQuery<Offer> query = mock(PanacheQuery.class);

        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(query.page(any(Page.class))).thenReturn(query);
        when(query.stream()).thenReturn(Stream.of(offer));
        when(query.count()).thenReturn(1L);
        when(query.pageCount()).thenReturn(1);

        when(offerRepository.findAllPaged(0, 10)).thenReturn(query);

        PaginatedResponse<OfferDTO> response1 = offerService.getAllOffer(0, 10);
        assertEquals(1, response1.getItems().size());
        assertEquals(1L, response1.getTotalItems());

        PaginatedResponse<OfferDTO> response2 = offerService.getAllOffer(0, 10);
        assertEquals(1, response2.getItems().size());
        assertEquals(1L, response2.getTotalItems());

        verify(offerRepository, times(1)).findAllPaged(0, 10);
    }

    @Test
    void testFindOfferById_CacheEnabled() {
        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(offerRepository.findByIdOptional(1L)).thenReturn(Optional.of(offer));

        OfferDTO offerDTO1 = offerService.findOfferById(1L);
        verify(offerRepository, times(1)).findByIdOptional(1L);

        OfferDTO offerDTO2 = offerService.findOfferById(1L);
        verify(offerRepository, times(1)).findByIdOptional(1L); // Repository je pozvan samo jednom
        assertEquals(offerDTO1, offerDTO2);
    }

    @Test
    @Transactional
    void testUpdateOffer_CacheInvalidation() {
        Offer existingOffer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        Car car = new Car.Builder()
                .id(2L)
                .brand("Audi")
                .model("A6")
                .build();

        when(offerRepository.findOfferWithCarById(1L)).thenReturn(Optional.of(existingOffer));
        when(carRepository.findByIdOptional(2L)).thenReturn(Optional.of(car));

        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Luka", "Smith", BigDecimal.valueOf(20000), 2L);
        OfferDTO updatedOffer = offerService.updateOffer(1L, updateOfferDTO);

        assertNotNull(updatedOffer);
        assertEquals("Smith", updatedOffer.customerLastName());
        assertEquals(2L, updatedOffer.carId());

        Cache offerCache = cacheManager.getCache("offer-cache").get();
        Object cachedValue = offerCache.get(1L, k -> null).await().indefinitely();
        assertNull(cachedValue);
    }

    @Test
    @Transactional
    void testDeleteOffer_CacheInvalidation() {
        Offer existingOffer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(offerRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingOffer));

        offerService.deleteOffer(1L);

        verify(offerRepository, times(1)).delete(existingOffer);

        Cache offerCache = cacheManager.getCache("offer-cache").get();
        Object cachedValue = offerCache.get(1L, k -> null).await().indefinitely();
        assertNull(cachedValue);
    }

    @Test
    void testSearchOffers_WithCriteria() {
        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        OfferSearchCriteria criteria = new OfferSearchCriteria("Luka", "Borna", 5000.00,
                15000.00, null, null, "id", true, 0, 10);

        when(offerRepository.searchOffers(criteria)).thenReturn(List.of(offer));

        List<OfferDTO> result = offerService.searchOffers(criteria);

        assertEquals(1, result.size());
        assertEquals("Luka", result.getFirst().customerFirstName());
        assertEquals("Borna", result.getFirst().customerLastName());
    }

    @Test
    void testSearchOffers_NoCriteriaProvided() {
        OfferSearchCriteria criteria = new OfferSearchCriteria(null, null, null, null, null, null, null, true, 0, 10);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            offerService.searchOffers(criteria);
        });

        assertEquals("At least one search parameter must be provided.", thrown.getMessage());
    }

    @Test
    void testFindOfferById_OfferNotFound() {
        when(offerRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        OfferNotFoundException thrown = assertThrows(OfferNotFoundException.class, () -> {
            offerService.findOfferById(999L);
        });

        assertEquals("offer with ID 999 not found", thrown.getMessage());
    }

    @Test
    @Transactional
    void testUpdateOffer_CarNotFound() {
        Offer existingOffer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(offerRepository.findOfferWithCarById(1L)).thenReturn(Optional.of(existingOffer));
        when(carRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Luka", "Smith", BigDecimal.valueOf(20000), 999L);

        CarNotFoundException thrown = assertThrows(CarNotFoundException.class, () -> {
            offerService.updateOffer(1L, updateOfferDTO);
        });

        assertEquals("Car with ID 999 not found", thrown.getMessage());
    }

    @Test
    @Transactional
    void testDeleteOffer_OfferNotFound() {
        when(offerRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () -> {
            offerService.deleteOffer(999L);
        });

        assertEquals("Offer with ID 999 not found", thrown.getMessage());
    }

    @Test
    void testSearchOffers_NoCriteriaProvided_ThrowsException() {
        OfferSearchCriteria emptyCriteria = new OfferSearchCriteria(null, null, null, null, null, null, null, true, 0, 10);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            offerService.searchOffers(emptyCriteria);
        });

        assertEquals("At least one search parameter must be provided.", thrown.getMessage());
    }


    @Test
    void testValidatePrices_NegativeValues_ThrowsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            offerService.validatePrices(BigDecimal.valueOf(-100), BigDecimal.valueOf(200));
        });

        assertEquals("Both minPrice and maxPrice must be positive numbers.", thrown.getMessage());
    }

    @Test
    void testValidatePrices_MinGreaterThanMax_ThrowsException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            offerService.validatePrices(BigDecimal.valueOf(300), BigDecimal.valueOf(200));
        });

    }




}