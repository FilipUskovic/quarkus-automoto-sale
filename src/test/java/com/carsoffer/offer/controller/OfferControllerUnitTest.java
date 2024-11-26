package com.carsoffer.offer.controller;

import com.carsoffer.common.exceptions.dto.ErrorResponse;
import com.carsoffer.common.exceptions.GlobalExceptionHandler;
import com.carsoffer.common.exceptions.OfferNotFoundException;
import com.carsoffer.common.utils.PaginatedResponse;
import com.carsoffer.offer.dto.CreateOfferDTO;
import com.carsoffer.offer.dto.OfferDTO;
import com.carsoffer.offer.dto.OfferSearchCriteria;
import com.carsoffer.offer.dto.UpdateOfferDTO;
import com.carsoffer.offer.service.OfferServiceImpl;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferControllerUnitTest {
    @Mock
    private OfferServiceImpl offerService;

    @InjectMocks
    private OfferController offerController;

    @Test
    void testGetOfferById_Success() {
        OfferDTO mockOffer = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);
        when(offerService.findOfferById(1L)).thenReturn(mockOffer);

        Response response = offerController.getOfferById(1L);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        OfferDTO offerResponse = (OfferDTO) response.getEntity();
        assertEquals("Luka", offerResponse.customerFirstName());
        assertEquals(2L, offerResponse.carId());
    }


    @Test
    void testDeleteOffer_Success() {
        doNothing().when(offerService).deleteOffer(1L);
        Response response = offerController.deleterOffer(1L);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        Map<String, String> responseBody = (Map<String, String>) response.getEntity();
        assertEquals("Successfully deleted offer!", responseBody.get("message"));

        verify(offerService, times(1)).deleteOffer(1L);
    }

    @Test
    void testCreateOffer_Success() {
        CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(10000), 2L);
        OfferDTO createdOfferDTO = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000), LocalDateTime.now(), LocalDateTime.now(), 2L);

        when(offerService.createOffer(any(CreateOfferDTO.class))).thenReturn(createdOfferDTO);

        Response response = offerController.createOffer(createOfferDTO);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        Map<String, Object> responseBody = (Map<String, Object>) response.getEntity();

        assertEquals("Offer successfully created", responseBody.get("message"));

        OfferDTO offerResponse = (OfferDTO) responseBody.get("offer");
        assertEquals("Luka", offerResponse.customerFirstName());
        assertEquals(2L, offerResponse.carId());
    }





    @Test
    public void testValidatePrices_InvalidRangeDirectCall() {
        OfferServiceImpl service = new OfferServiceImpl(null, null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.validatePrices(BigDecimal.valueOf(500), BigDecimal.valueOf(100));
        });

        assertEquals("minPrice must be less than or equal to maxPrice.", exception.getMessage());
    }


    @Test
    public void testHandleIllegalArgumentException() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        IllegalArgumentException ex = new IllegalArgumentException("Test exception");
        Response response = globalExceptionHandler.toResponse(ex);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        assertTrue(response.getEntity() instanceof ErrorResponse);

        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertEquals("Invalid argument", errorResponse.getMessage());
        assertTrue(errorResponse.getDetails().contains("Test exception"));
    }

    @Test
    void testUpdateOffer_Success() {
        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Jane", "Borna", BigDecimal.valueOf(15000), 3L);
        OfferDTO updatedOfferDTO = new OfferDTO(1L, "Jane", "Borna", BigDecimal.valueOf(15000), LocalDateTime.now(), LocalDateTime.now(), 3L);

        when(offerService.updateOffer(eq(1L), any(UpdateOfferDTO.class))).thenReturn(updatedOfferDTO);

        Response response = offerController.updateOffer(1L, updateOfferDTO);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        Map<String, Object> responseBody = (Map<String, Object>) response.getEntity();
        OfferDTO offerResponse = (OfferDTO) responseBody.get("offer");

        assertEquals("Jane", offerResponse.customerFirstName());
        assertEquals(3L, offerResponse.carId());
    }

    @Test
    void testGetAllOffers_Success() {
        OfferDTO offerDTO = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);
        PaginatedResponse<OfferDTO> paginatedResponse = new PaginatedResponse<>(List.of(offerDTO), 1, 1, 0, 10);

        when(offerService.getAllOffer(0, 10)).thenReturn(paginatedResponse);

        Response response = offerController.getAllOffers(0, 10);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        PaginatedResponse<OfferDTO> responseBody = (PaginatedResponse<OfferDTO>) response.getEntity();
        assertEquals(1, responseBody.getTotalItems());
        assertEquals(1, responseBody.getTotalPages());
        List<OfferDTO> offers = responseBody.getItems();
        assertEquals(1, offers.size());
        assertEquals("Luka", offers.getFirst().customerFirstName());
    }


    @Test
    void testGetOfferById_NotFound() {
        when(offerService.findOfferById(999L)).thenThrow(new OfferNotFoundException(999L));

        try {
            offerController.getOfferById(999L);
            fail("Expected OfferNotFoundException to be thrown");
        } catch (OfferNotFoundException e) {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();
            Response response = handler.toResponse(e);
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertTrue(response.getEntity() instanceof ErrorResponse, "Expected ErrorResponse entity");

            ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
            assertEquals("Offer not found", errorResponse.getMessage());
            assertEquals("Offer with ID 999 not found", errorResponse.getDetails());
        }
    }


    @Test
    void testCreateOffer_InvalidData() {
        when(offerService.findOfferById(999L)).thenThrow(new OfferNotFoundException(999L));
        try {
            offerController.getOfferById(999L);
            fail("Expected OfferNotFoundException to be thrown");
        } catch (OfferNotFoundException e) {
            Response response = handleException(e);
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertTrue(response.getEntity() instanceof ErrorResponse, "Expected ErrorResponse entity");

            ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
            assertEquals("Offer not found", errorResponse.getMessage());
            assertEquals("Offer with ID 999 not found", errorResponse.getDetails());
        }
    }

    @Test
    void testUpdateOffer_NotFound() {
        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Jane", "Borna", BigDecimal.valueOf(15000), 3L);
        when(offerService.updateOffer(eq(999L), any(UpdateOfferDTO.class)))
                .thenThrow(new OfferNotFoundException(999L));

        try {
            offerController.updateOffer(999L, updateOfferDTO);
            fail("Expected OfferNotFoundException to be thrown");
        } catch (OfferNotFoundException e) {
            Response response = handleException(e);
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

            assertTrue(response.getEntity() instanceof ErrorResponse, "Expected ErrorResponse entity");

            ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
            assertEquals("Offer not found", errorResponse.getMessage());
            assertEquals("Offer with ID 999 not found", errorResponse.getDetails());
        }
    }


    @Test
    void testUpdateOffer_InvalidData() {
        UpdateOfferDTO invalidUpdateOfferDTO = new UpdateOfferDTO("", "", BigDecimal.valueOf(-5000), -1L);
        when(offerService.updateOffer(any(Long.class), any(UpdateOfferDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid data"));

        try {
            offerController.updateOffer(1L, invalidUpdateOfferDTO);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            Response response = handleException(e);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertTrue(response.getEntity() instanceof ErrorResponse, "Expected ErrorResponse entity");

            ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
            assertEquals("Invalid argument", errorResponse.getMessage());
            assertEquals("Invalid data", errorResponse.getDetails());
        }
    }


    @Test
    void testDeleteOffer_InvalidId() {
        doThrow(new IllegalArgumentException("Invalid offer ID")).when(offerService).deleteOffer(-1L);

        try {
            offerController.deleterOffer(-1L);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            Response response = handleException(e);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertInstanceOf(ErrorResponse.class, response.getEntity(), "Expected ErrorResponse entity");

            ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
            assertEquals("Invalid argument", errorResponse.getMessage());
            assertEquals("Invalid offer ID", errorResponse.getDetails());
        }
    }



    @Test
    void testGetAllOffers_InvalidPagination() {
        when(offerService.getAllOffer(-1, 10))
                .thenThrow(new IllegalArgumentException("Page number cannot be negative"));

        try {
            offerController.getAllOffers(-1, 10);
        } catch (IllegalArgumentException e) {
            Response response = handleException(e);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertInstanceOf(ErrorResponse.class, response.getEntity(), "Expected ErrorResponse entity");

            ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
            assertEquals("Invalid argument", errorResponse.getMessage());
            assertEquals("Page number cannot be negative", errorResponse.getDetails());
        }
    }

    @Test
    void testOfferSearchCriteria_InvalidPagination() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new OfferSearchCriteria(
                    "Luka",
                    "Borna",
                    5000.0,
                    10000.0,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31),
                    "price",
                    true,
                    -1,
                    10
            );
        });

        assertEquals("Page and size must be valid positive numbers.", exception.getMessage());
    }

    @Test
    void testCreateOffer_MissingFields() {
        CreateOfferDTO missingFieldsCreateOfferDTO = new CreateOfferDTO("", "", BigDecimal.valueOf(10000), 2L);

        when(offerService.createOffer(any(CreateOfferDTO.class)))
                .thenThrow(new IllegalArgumentException("Missing required fields"));

        try {
            offerController.createOffer(missingFieldsCreateOfferDTO);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            Response response = handleException(e);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            assertInstanceOf(ErrorResponse.class, response.getEntity(), "Expected ErrorResponse entity");

            ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
            assertEquals("Invalid argument", errorResponse.getMessage());
            assertEquals("Missing required fields", errorResponse.getDetails());
        }
    }

    @Test
    void testOfferSearchCriteria_DefaultSortBy() {
        OfferSearchCriteria criteria = new OfferSearchCriteria(
                "Luka",
                "Borna",
                5000.0,
                10000.0,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                null,
                true,
                0,
                10
        );

        assertEquals("id", criteria.sortBy());
    }


    @Test
    void testOfferSearchCriteria_ValidDateRange() {
        OfferSearchCriteria criteria = new OfferSearchCriteria(
                "Jane",
                "Borna",
                5000.0,
                15000.0,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "price",
                false,
                0,
                10
        );

        assertEquals("price", criteria.sortBy());
        assertFalse(criteria.asc());
        assertEquals(0, criteria.page());
        assertEquals(10, criteria.size());
    }


    private Response handleException(Throwable exception) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        return handler.toResponse(exception);
    }

}