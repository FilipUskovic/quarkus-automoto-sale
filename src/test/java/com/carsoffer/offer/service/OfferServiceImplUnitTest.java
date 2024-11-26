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
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OfferServiceImplUnitTest {

    @Mock
    OfferRepository offerRepository;

    @Mock
    CarRepository carRepository;

    @InjectMocks
    OfferServiceImpl offerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateOffer_CarNotFoundd() {
        when(carRepository.findByIdOptional(999L)).thenReturn(Optional.empty());

        ArgumentCaptor<CreateOfferDTO> captor = ArgumentCaptor.forClass(CreateOfferDTO.class);

        CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(10000), 999L);

        CarNotFoundException thrown = assertThrows(CarNotFoundException.class, () -> {
            offerService.createOffer(createOfferDTO);
        });

        assertEquals("Car with ID 999 not found", thrown.getMessage());

        verify(offerRepository, never()).persist(any(Offer.class));
    }


    @Test
    void testUpdateOffer_OfferNotFound() {
        when(offerRepository.findOfferWithCarById(999L)).thenReturn(Optional.empty());

        ArgumentCaptor<UpdateOfferDTO> captor = ArgumentCaptor.forClass(UpdateOfferDTO.class);

        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Luka", "Borna", BigDecimal.valueOf(20000), 1L);

        OfferNotFoundException thrown = assertThrows(OfferNotFoundException.class, () -> {
            offerService.updateOffer(999L, updateOfferDTO);
        });

        assertEquals("offer with ID 999 not found", thrown.getMessage());
        verify(offerRepository, never()).persist(any(Offer.class));
    }



    @Test
    void testDeleteOffer() {
        Offer existingOffer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(offerRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingOffer));

        ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);

        offerService.deleteOffer(1L);
        verify(offerRepository).delete(captor.capture());

        Offer deletedOffer = captor.getValue();
        assertEquals(existingOffer.getId(), deletedOffer.getId());
    }

    @Test
    void testSearchOffers_WithCriteria() {
        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        OfferSearchCriteria criteria = new OfferSearchCriteria("Luka", "Borna", 5000.0, 15000.0, null, null, "id", true, 0, 10);
        when(offerRepository.searchOffers(criteria)).thenReturn(List.of(offer));

        ArgumentCaptor<OfferSearchCriteria> captor = ArgumentCaptor.forClass(OfferSearchCriteria.class);

        List<OfferDTO> result = offerService.searchOffers(criteria);

        assertEquals(1, result.size());
        assertEquals("Luka", result.getFirst().customerFirstName());

        verify(offerRepository).searchOffers(captor.capture());

        OfferSearchCriteria capturedCriteria = captor.getValue();
        assertEquals("Luka", capturedCriteria.customerFirstName());
    }



    @Test
    void testUpdateOffer_Success() {
            Car car = new Car();
            car.setId(1L);

            Offer existingOffer = new Offer.Builder()
                    .id(1L)
                    .customerFirstName("Luka")
                    .customerLastName("Borna")
                    .price(BigDecimal.valueOf(10000))
                    .car(car)
                    .build();

            when(offerRepository.findOfferWithCarById(1L)).thenReturn(Optional.of(existingOffer));
            when(carRepository.findByIdOptional(1L)).thenReturn(Optional.of(car));

            UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Luka", "Borna", BigDecimal.valueOf(20000), 1L);
            OfferDTO updatedOffer = offerService.updateOffer(1L, updateOfferDTO);

            assertEquals("Luka", updatedOffer.customerFirstName());
            assertEquals(BigDecimal.valueOf(20000), updatedOffer.price());

            assertEquals("Luka", existingOffer.getCustomerFirstName());
            assertEquals(BigDecimal.valueOf(20000), existingOffer.getPrice());
            assertEquals(car, existingOffer.getCar());  
        }

    @Test
    void testDeleteOffer_Success() {
        Offer existingOffer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(offerRepository.findByIdOptional(1L)).thenReturn(Optional.of(existingOffer));

        offerService.deleteOffer(1L);

        ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);
        verify(offerRepository).delete(captor.capture());

        Offer deletedOffer = captor.getValue();
        assertEquals(existingOffer.getId(), deletedOffer.getId());
    }

    @Test
    void testFindOfferById_Success() {
        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        when(offerRepository.findByIdOptional(1L)).thenReturn(Optional.of(offer));

        OfferDTO foundOffer = offerService.findOfferById(1L);
        assertNotNull(foundOffer);
        assertEquals("Luka", foundOffer.customerFirstName());

        verify(offerRepository, times(1)).findByIdOptional(1L);
    }

    @Test
    void testSearchOffers_Success() {
        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        OfferSearchCriteria criteria = new OfferSearchCriteria("Luka", "Brona", 5000.0, 15000.0, null, null, "id", true, 0, 10);
        when(offerRepository.searchOffers(criteria)).thenReturn(List.of(offer));

        List<OfferDTO> offers = offerService.searchOffers(criteria);
        assertEquals(1, offers.size());
        assertEquals("Luka", offers.getFirst().customerFirstName());

        ArgumentCaptor<OfferSearchCriteria> captor = ArgumentCaptor.forClass(OfferSearchCriteria.class);
        verify(offerRepository).searchOffers(captor.capture());

        OfferSearchCriteria capturedCriteria = captor.getValue();
        assertEquals("Luka", capturedCriteria.customerFirstName());
    }

    @Test
    void testGetOffersByCustomerName_Success() {
        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        PanacheQuery<Offer> query = mock(PanacheQuery.class);

        when(query.stream()).thenReturn(Stream.of(offer));
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.count()).thenReturn(1L);

        when(offerRepository.findByCustomerByFirstNameAndCustomerByLastNamePaged("Luka", "Borna", 0, 10))
                .thenReturn(query);

        PaginatedResponse<OfferDTO> response = offerService.getOffersByCustomerName("Luka", "Borna", 0, 10);
        assertEquals(1, response.getItems().size());
        assertEquals("Luka", response.getItems().getFirst().customerFirstName());

        verify(offerRepository, times(1))
                .findByCustomerByFirstNameAndCustomerByLastNamePaged("Luka", "Borna", 0, 10);
    }

    @Test
    void testGetOffersByPriceRange_Success() {
        Offer offer = new Offer.Builder()
                .id(1L)
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(10000))
                .build();

        PanacheQuery<Offer> mockQuery = mockPanacheQuery(List.of(offer));

        when(offerRepository.findByPriceBetweenPaged(BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), 0, 10))
                .thenReturn(mockQuery);
        PaginatedResponse<OfferDTO> response = offerService.getOffersByPriceRange(BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), 0, 10);
        assertEquals(1, response.getItems().size());
        assertEquals(BigDecimal.valueOf(10000), response.getItems().getFirst().price());
    }

    @Test
    void testValidatePrices_Success() {
        assertDoesNotThrow(() -> offerService.validatePrices(BigDecimal.valueOf(100), BigDecimal.valueOf(200)));
    }

    @Test
    void testValidatePrices_InvalidRange() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            offerService.validatePrices(BigDecimal.valueOf(300), BigDecimal.valueOf(200));
        });
        assertEquals("minPrice must be less than or equal to maxPrice.", thrown.getMessage());
    }

    private PanacheQuery<Offer> mockPanacheQuery(List<Offer> results) {
        PanacheQuery<Offer> query = mock(PanacheQuery.class);
        when(query.stream()).thenReturn(results.stream());
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.count()).thenReturn((long) results.size());
        return query;
    }

}