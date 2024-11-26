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
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class OfferControllerHybridTest {

    @InjectMock
    OfferServiceImpl offerService;

    @Test
    void testGetAllOffers_Success() {

        OfferDTO offerDTO = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);        PaginatedResponse<OfferDTO> paginatedResponse = new PaginatedResponse<>(List.of(offerDTO), 1, 1, 0, 10);

        when(offerService.getAllOffer(0, 10)).thenReturn(paginatedResponse);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/offers")
                .then()
                .statusCode(200)
                .body("items.size()", is(1))
                .body("items[0].customerFirstName", is("Luka"))
                .body("totalItems", is(1))
                .body("totalPages", is(1));

        verify(offerService, times(1)).getAllOffer(0, 10);
    }

    @Test
    void testGetOfferById_Success() {
        OfferDTO offerDTO = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);

        when(offerService.findOfferById(1L)).thenReturn(offerDTO);

        given()
                .when()
                .get("/offers/{id}", 1L)
                .then()
                .statusCode(200)
                .body("customerFirstName", is("Luka"))
                .body("carId", is(2));

        verify(offerService, times(1)).findOfferById(1L);
    }

    @Test
    void testGetOfferById_NotFound() {
        when(offerService.findOfferById(999L)).thenThrow(new OfferNotFoundException(999L));

        given()
                .when()
                .get("/offers/{id}", 999L)
                .then()
                .statusCode(404)
                .body("message", is("Offer not found"));

        verify(offerService, times(1)).findOfferById(999L);
    }



    @Test
    void testDeleteOffer_Success() {
        doNothing().when(offerService).deleteOffer(1L);

        given()
                .when()
                .delete("/offers/{id}", 1L)
                .then()
                .statusCode(200)
                .body("message", is("Successfully deleted offer!"));

        verify(offerService, times(1)).deleteOffer(1L);
    }

    @Test
    void testDeleteOffer_NotFound() {
        doThrow(new OfferNotFoundException(999L)).when(offerService).deleteOffer(999L);

        given()
                .when()
                .delete("/offers/{id}", 999L)
                .then()
                .statusCode(404)
                .body("message", is("Offer not found"));

        verify(offerService, times(1)).deleteOffer(999L);
    }

    @Test
    void testCreateOffer_Success() {
        CreateOfferDTO createOfferDTO = new CreateOfferDTO("Luka", "Borna", BigDecimal.valueOf(10000), 2L);
        OfferDTO createdOfferDTO = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);

        when(offerService.createOffer(any(CreateOfferDTO.class))).thenReturn(createdOfferDTO);

        given()
                .contentType("application/json")
                .body(createOfferDTO)
                .when()
                .post("/offers")
                .then()
                .statusCode(201)
                .body("offer.customerFirstName", is("Luka"))
                .body("offer.carId", is(2));

        verify(offerService, times(1)).createOffer(any(CreateOfferDTO.class));
    }

    @Test
    void testUpdateOffer_Success() {
        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Jane", "Borna", BigDecimal.valueOf(15000), 3L);
        OfferDTO updatedOfferDTO = new OfferDTO(1L, "Jane", "Borna", BigDecimal.valueOf(15000),
                LocalDateTime.now(), LocalDateTime.now(), 3L);

        when(offerService.updateOffer(eq(1L), any(UpdateOfferDTO.class))).thenReturn(updatedOfferDTO);

        given()
                .contentType("application/json")
                .body(updateOfferDTO)
                .when()
                .put("/offers/{id}", 1L)
                .then()
                .statusCode(200)
                .body("offer.customerFirstName", is("Jane"))
                .body("offer.carId", is(3));

        verify(offerService, times(1)).updateOffer(eq(1L), any(UpdateOfferDTO.class));
    }

    @Test
    void testSearchOffers_Success() {
        OfferDTO offerDTO = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);

        when(offerService.searchOffers(any())).thenReturn(List.of(offerDTO));

        given()
                .queryParam("customerFirstName", "Luka")
                .queryParam("customerLastName", "Borna")
                .queryParam("minPrice", 5000)
                .queryParam("maxPrice", 15000)
                .queryParam("sortBy", "price")
                .queryParam("asc", true)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/offers/search")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].customerFirstName", is("Luka"))
                .body("[0].carId", is(2));

        verify(offerService, times(1)).searchOffers(any());
    }


    @Test
    void testCreateOffer_InvalidData() {
        CreateOfferDTO invalidCreateOfferDTO = new CreateOfferDTO("", "", BigDecimal.valueOf(-100), 0L);

        given()
                .contentType("application/json")
                .body(invalidCreateOfferDTO)
                .when()
                .post("/offers")
                .then()
                .statusCode(400)
                .body("errors.size()", is(3))
                .body("errors.field", hasItems("customerFirstName", "customerLastName", "price"))
                .body("errors.message", hasItems(
                        "FirstName must not be empty",
                        "Price must be positive",
                        "LastName must not be empty"
                ));

        verify(offerService, never()).createOffer(any(CreateOfferDTO.class));
    }


    @Test
    void testUpdateOffer_InvalidData() {
        UpdateOfferDTO invalidUpdateOfferDTO = new UpdateOfferDTO("", "", BigDecimal.valueOf(-5000), -1L);

        given()
                .contentType("application/json")
                .body(invalidUpdateOfferDTO)
                .when()
                .put("/offers/{id}", 1L)
                .then()
                .statusCode(400)
                .body("errors.size()", is(3))
                .body("errors.field", hasItems("customerFirstName", "customerLastName", "price"))
                .body("errors.message", hasItems(
                        "FirstName must not be empty",
                        "LastName must not be empty",
                        "Price must be positive"
                ));

        verify(offerService, never()).updateOffer(any(Long.class), any(UpdateOfferDTO.class));
    }


    @Test
    void testSearchOffers_InvalidDateRange() {
        given()
                .queryParam("startDate", "2024-09-30")
                .queryParam("endDate", "2024-09-01")
                .when()
                .get("/offers/search")
                .then()
                .statusCode(400)
                .body("message", is("Start date cannot be after end date."));

        verify(offerService, never()).searchOffers(any());
    }

    @Test
    void testSearchOffers_ValidSearchCriteria() {
        OfferDTO offerDTO = new OfferDTO(1L, "Jane", "Borna", BigDecimal.valueOf(15000),
                LocalDateTime.now(), LocalDateTime.now(), 3L);

        when(offerService.searchOffers(any())).thenReturn(List.of(offerDTO));

        given()
                .queryParam("customerFirstName", "Jane")
                .queryParam("customerLastName", "Borna")
                .queryParam("minPrice", 10000)
                .queryParam("maxPrice", 20000)
                .queryParam("startDate", "2024-09-01")
                .queryParam("endDate", "2024-09-30")
                .queryParam("sortBy", "price")
                .queryParam("asc", true)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/offers/search")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].customerFirstName", is("Jane"))
                .body("[0].carId", is(3));

        verify(offerService, times(1)).searchOffers(any());
    }

    @Test
    void testSearchOffers_ValidRequest() {
        OfferDTO offerDTO = new OfferDTO(1L, "Jane", "Borna", BigDecimal.valueOf(15000),
                LocalDateTime.of(2023, 9, 1, 12, 12, 12), LocalDateTime.now(), 3L);

        when(offerService.searchOffers(any())).thenReturn(List.of(offerDTO));

        given()
                .queryParam("customerFirstName", "Luka")
                .queryParam("customerLastName", "Borna")
                .queryParam("minPrice", 1000.0)
                .queryParam("maxPrice", 6000.0)
                .queryParam("startDate", "2023-09-01")
                .queryParam("endDate", "2023-09-30")
                .queryParam("sortBy", "price")
                .queryParam("asc", "true")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/offers/search")
                .then()
                .log().all()
                .statusCode(200)
                .body("[0].id", is(1))
                .body("[0].customerFirstName", is("Jane"))
                .body("[0].customerLastName", is("Borna"))
                .body("[0].price", is(15000))
                .body("[0].offerDate", is("2023-09-01T12:12:12"))
                .body("[0].carId", is(3));

        verify(offerService, times(1)).searchOffers(any());
    }

    @Test
    void testDeleteOffer_NotFound_ExceptionHandler() {
        doThrow(new OfferNotFoundException(999L)).when(offerService).deleteOffer(999L);

        given()
                .when()
                .delete("/offers/{id}", 999L)
                .then()
                .statusCode(404)
                .body("message", is("Offer not found"));

        verify(offerService, times(1)).deleteOffer(999L);
    }

    @Test
    void testGetAllOffers_EmptyList() {
        PaginatedResponse<OfferDTO> emptyResponse = new PaginatedResponse<>(List.of(), 0, 1, 0, 10);
        when(offerService.getAllOffer(0, 10)).thenReturn(emptyResponse);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/offers")
                .then()
                .statusCode(404)
                .body("message", equalTo("Nema dostupnih ponuda."))
                .body("details", equalTo("Nema ponuda dostupnih za traženi upit."));

        verify(offerService, times(1)).getAllOffer(0, 10);
    }

    @Test
    void testUpdateOffer_NotFound() {
        UpdateOfferDTO updateOfferDTO = new UpdateOfferDTO("Luka", "Smith", BigDecimal.valueOf(20000), 2L);
        doThrow(new OfferNotFoundException(999L)).when(offerService).updateOffer(eq(999L), any(UpdateOfferDTO.class));

        given()
                .contentType("application/json")
                .body(updateOfferDTO)
                .when()
                .put("/offers/{id}", 999L)
                .then()
                .statusCode(404)
                .body("message", is("Offer not found"));

        verify(offerService, times(1)).updateOffer(eq(999L), any(UpdateOfferDTO.class));
    }


    @Test
    void testSearchOffers_NoOffersFound() {
        when(offerService.searchOffers(any())).thenReturn(Collections.emptyList());

        given()
                .queryParam("customerFirstName", "Luka")
                .queryParam("customerLastName", "Borna")
                .queryParam("minPrice", 100)
                .queryParam("maxPrice", 500)
                .queryParam("startDate", "2024-01-01")
                .queryParam("endDate", "2024-01-31")
                .queryParam("sortBy", "price")
                .queryParam("asc", true)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/offers/search")
                .then()
                .statusCode(404)
                .body("message", is("No offers found for the given search criteria."));

        verify(offerService).searchOffers(any());
    }

    @Test
    void testSearchOffers_InvalidDateFormat() {
        given()
                .queryParam("startDate", "invalid-date")
                .queryParam("endDate", "2024-01-31")
                .when()
                .get("/offers/search")
                .then()
                .log().all()
                .statusCode(400)
                .body("message", is("Invalid argument"))
                .body("details", is("Start date must be in the format 'YYYY-MM-DD'. Example: 2024-09-27."));

        verify(offerService, never()).searchOffers(any());
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
    void testSearchOffers_SortingAndPagination() {
        OfferDTO offer1 = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);
        OfferDTO offer2 = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);

        List<OfferDTO> mockOffers = List.of(offer1, offer2);

        when(offerService.searchOffers(any())).thenReturn(mockOffers);

        given()
                .queryParam("sortBy", "price")
                .queryParam("asc", false)
                .queryParam("page", 1)
                .queryParam("size", 5)
                .when()
                .get("/offers/search")
                .then()
                .statusCode(200)
                .body("[0].id", equalTo(offer1.id().intValue()))
                .body("[0].customerFirstName", equalTo(offer1.customerFirstName()))
                .body("[0].customerLastName", equalTo(offer1.customerLastName()))
                .body("[0].price", equalTo(offer1.price().intValue()));

        verify(offerService).searchOffers(any(OfferSearchCriteria.class));
    }

    @Test
    void testSearchOffers_NoDateParams() {
        OfferDTO offer1 = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);
        OfferDTO offer2 = new OfferDTO(1L, "Luka", "Borna", BigDecimal.valueOf(10000),
                LocalDateTime.now(), LocalDateTime.now(), 2L);
        List<OfferDTO> mockOffers = List.of(offer1, offer2);

        when(offerService.searchOffers(any())).thenReturn(mockOffers);

        given()
                .queryParam("minPrice", 100)
                .queryParam("maxPrice", 500)
                .when()
                .get("/offers/search")
                .then()
                .statusCode(200)
                .body("[0].id", equalTo(offer1.id().intValue()))
                .body("[1].id", equalTo(offer2.id().intValue()));

        verify(offerService).searchOffers(any(OfferSearchCriteria.class));
    }
}