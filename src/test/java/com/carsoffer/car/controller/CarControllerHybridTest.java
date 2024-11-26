package com.carsoffer.car.controller;

import com.carsoffer.car.dto.CarDTO;
import com.carsoffer.car.dto.CreateCarDTO;
import com.carsoffer.car.dto.FuelType;
import com.carsoffer.car.dto.UpdateCarDTO;
import com.carsoffer.car.service.CarService;
import com.carsoffer.common.exceptions.CarNotFoundException;
import com.carsoffer.common.utils.PaginatedResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class CarControllerHybridTest {

    @InjectMock
    CarService carService;

    @Test
    void testCreateCar_Success() {
        CreateCarDTO carDTO = new CreateCarDTO("BMW", "X5", 2022, "Black", FuelType.PETROL, "123qwe123qwe123qw");

        when(carService.createCar(any(CreateCarDTO.class))).thenReturn(new CarDTO(1L, "BMW", "X5", 2022, "Black", FuelType.PETROL, "123qwe123qwe123qw"));

        given()
                .contentType(ContentType.JSON)
                .body(carDTO)
                .when()
                .post("/cars")
                .then()
                .statusCode(201)
                .body("car.brand", is("BMW"))
                .body("car.model", is("X5"));
        verify(carService, times(1)).createCar(any(CreateCarDTO.class));
    }

        @Test
        void testCreateCar_InvalidData () {
            CreateCarDTO invalidCarDTO = new CreateCarDTO("", "X5", 2022, "Black", FuelType.PETROL, "123qwe123qwe123qw");
            given()
                    .contentType(ContentType.JSON)
                    .body(invalidCarDTO)
                    .when()
                    .post("/cars")
                    .then()
                    .statusCode(400)
                    .body("errors.size()", is(1))
                    .body("errors[0].field", is("brand"))
                    .body("errors[0].message", is("Brand must not be empty"));

            verify(carService, never()).createCar(any(CreateCarDTO.class));
        }


    @Test
    void testCreateCar_InvalidData_EmptyFields() {
        CreateCarDTO invalidCarDTO = new CreateCarDTO("", "", 2022, "", FuelType.PETROL, "");

        given()
                .contentType(ContentType.JSON)
                .body(invalidCarDTO)
                .when()
                .post("/cars")
                .then()
                .statusCode(400)
                .body("errors.size()", is(5))
                .body("errors.field", hasItems("brand", "model", "color", "vin"))
                .body("errors.message", hasItems(
                        "Brand must not be empty",
                        "Model must not be empty",
                        "Color must not be empty",
                        "Vin must not be empty",
                        "VIN must be exactly 17 characters"));

        verify(carService, never()).createCar(any(CreateCarDTO.class));

    }

    @Test
    void testCreateCar_InvalidData_SpecialCharactersInBrand() {
        CreateCarDTO invalidCarDTO = new CreateCarDTO("@BMW", "X5", 2022, "Black", FuelType.PETROL, "123qwe123qwe123qw");

        given()
                .contentType(ContentType.JSON)
                .body(invalidCarDTO)
                .when()
                .post("/cars")
                .then()
                .statusCode(400)
                .body("errors.size()", is(1))
                .body("errors[0].field", is("brand"))
                .body("errors[0].message", is("Brand can contain alphanumeric characters, spaces, hyphens, and underscores, and must be between 1 and 50 characters"));

        verify(carService, never()).createCar(any(CreateCarDTO.class));
    }

    @Test
    void testCreateCar_InvalidData_ExceedingMaxLength() {
        CreateCarDTO invalidCarDTO = new CreateCarDTO("BMW".repeat(20), "X5", 2022, "Black", FuelType.PETROL, "123qwe123qwe123qw");

        given()
                .contentType(ContentType.JSON)
                .body(invalidCarDTO)
                .when()
                .post("/cars")
                .then()
                .statusCode(400)
                .body("errors.size()", is(1))
                .body("errors[0].field", is("brand"))
                .body("errors[0].message", is("Brand can contain alphanumeric characters, spaces, hyphens, and underscores, and must be between 1 and 50 characters"));

        verify(carService, never()).createCar(any(CreateCarDTO.class));
    }

    @Test
    void testGetCarById_Success() {
        CarDTO carDTO = new CarDTO(1L, "Audi", "A4", 2020, "Blue", FuelType.PETROL, "123qwe123qwe123qw");
        when(carService.getCarById(1L)).thenReturn(carDTO);
        given()
                .when()
                .get("/cars/{id}", 1L)
                .then()
                .statusCode(200)
                .body("brand", is("Audi"))
                .body("model", is("A4"));

        verify(carService, times(1)).getCarById(1L);
    }

    @Test
    void testGetCarById_NotFound() {
        when(carService.getCarById(999L)).thenThrow(new CarNotFoundException(999L));

        given()
                .when()
                .get("/cars/{id}", 999L)
                .then()
                .statusCode(404)
                .body("message", is("Car not found"));  // Adjust the expected message

        verify(carService, times(1)).getCarById(999L);
    }

    @Test
    void testDeleteCar_Success() {
        doNothing().when(carService).deleteCar(1L);

        given()
                .when()
                .delete("/cars/{id}", 1L)
                .then()
                .statusCode(200);

        verify(carService, times(1)).deleteCar(1L);
    }

    @Test
    void testDeleteCar_NotFound() {
        doThrow(new CarNotFoundException(999L)).when(carService).deleteCar(999L);

        given()
                .when()
                .delete("/cars/{id}", 999L)
                .then()
                .statusCode(404)
                .body("message", is("Car not found"));

        verify(carService, times(1)).deleteCar(999L);
    }

    @Test
    void testUpdateCar_Success() {
        UpdateCarDTO updateCarDTO = new UpdateCarDTO("BMW", "X7", 2023, "White", FuelType.DIESEL);
        CarDTO updatedCarDTO = new CarDTO(1L, "BMW", "X7", 2023, "White", FuelType.DIESEL, "123qwe123qwe123qw");

        when(carService.updateCar(eq(1L), any(UpdateCarDTO.class))).thenReturn(updatedCarDTO);

        given()
                .contentType(ContentType.JSON)
                .body(updateCarDTO)
                .when()
                .put("/cars/{id}", 1L)
                .then()
                .statusCode(200)
                .body("car.id", is(1))
                .body("car.brand", is("BMW"))
                .body("car.model", is("X7"));

        verify(carService, times(1)).updateCar(eq(1L), any(UpdateCarDTO.class));
    }

    @Test
    void testUpdateCar_InvalidData() {
        UpdateCarDTO invalidUpdateCarDTO = new UpdateCarDTO("", "X7", 2023, "White", FuelType.DIESEL);

        given()
                .contentType(ContentType.JSON)
                .body(invalidUpdateCarDTO)
                .when()
                .put("/cars/{id}", 1L)
                .then()
                .statusCode(400)
                .body("errors[0].field", is("brand"))
                .body("errors[0].message", is("Brand must not be empty"));

        verify(carService, never()).updateCar(any(Long.class), any(UpdateCarDTO.class));
    }

    @Test
    void testGetCarById_NotFound_ExceptionHandler() {
        when(carService.getCarById(999L)).thenThrow(new CarNotFoundException(999L));

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/cars/999")
                .then()
                .statusCode(404)
                .body("message", is("Car not found"));

        verify(carService, times(1)).getCarById(999L);
    }

    @Test
    public void testGetAllCars_Success() {
        List<CarDTO> cars = List.of(
                new CarDTO(1L, "Audi", "A4", 2020, "Blue", FuelType.PETROL, "12212"),
                new CarDTO(2L, "BMW", "X5", 2022, "Black", FuelType.DIESEL, "12212")
        );

        PaginatedResponse<CarDTO> paginatedResponse = new PaginatedResponse<>(cars, 2, 1, 0, 20);

        when(carService.getAllCars(0, 20)).thenReturn(paginatedResponse);


        given()
                .when()
                .get("/cars/?page=0&pageSize=20")
                .then()
                .statusCode(200)
                .body("items.size()", is(2))
                .body("items[0].brand", is("Audi"))
                .body("items[1].brand", is("BMW"))
                .body("totalItems", is(2))
                .body("totalPages", is(1))
                .body("currentPage", is(0));
    }

    @Test
    void testDeleteCar_NotFound_ExceptionHandler() {
        doThrow(new CarNotFoundException(999L)).when(carService).deleteCar(999L);

        given()
                .when()
                .delete("/cars/999")
                .then()
                .statusCode(404)
                .body("message", is("Car not found"));

        verify(carService, times(1)).deleteCar(999L);
    }

    @Test
    void testUpdateCar_NotFound() {
        UpdateCarDTO updateCarDTO = new UpdateCarDTO("Toyota", "Camry", 2022, "White", FuelType.PETROL);
        doThrow(new CarNotFoundException(999L)).when(carService).updateCar(eq(999L), any(UpdateCarDTO.class));
        given()
                .contentType(ContentType.JSON)
                .body(updateCarDTO)
                .when()
                .put("/cars/{id}", 999L)
                .then()
                .statusCode(404)
                .body("message", is("Car not found"));

        verify(carService, times(1)).updateCar(eq(999L), any(UpdateCarDTO.class));
    }


    @Test
    void testGetAllCars_EmptyList() {
        PaginatedResponse<CarDTO> emptyResponse = new PaginatedResponse<>(List.of(), 0, 1, 0, 20);
        when(carService.getAllCars(0, 20)).thenReturn(emptyResponse);

        given()
                .when()
                .get("/cars/?page=0&pageSize=20")
                .then()
                .statusCode(404)
                .body("message", is("Nema rezultata."))
                .body("details", is("Nema automobila dostupnih za traženi upit."));

        verify(carService, times(1)).getAllCars(0, 20);
    }

    @Test
    void testGetAllCars_InvalidSizeParameter() {
        given()
                .queryParam("page", 0)
                .queryParam("pageSize", 0)
                .when()
                .get("/cars/")
                .then()
                .statusCode(400)
                .body("errors[0].field", is("pageSize"))
                .body("errors[0].message", is("must be greater than or equal to 1"));

        verify(carService, never()).getAllCars(anyInt(), anyInt());
    }

    @Test
    void testGetAllCars_InvalidPageParameter() {
        given()
                .queryParam("page", -1)
                .queryParam("size", 10)
                .when()
                .get("/cars/")
                .then()
                .statusCode(400)
                .body("errors[0].field", is("page"))
                .body("errors[0].message", is("must be greater than or equal to 0"));
        verify(carService, never()).getAllCars(anyInt(), anyInt());

    }


    @Test
    void testSearchCars_Success() {
        List<CarDTO> cars = List.of(
                new CarDTO(1L, "Toyota", "Camry", 2020, "Black", FuelType.PETROL, "12212"),
                new CarDTO(2L, "Toyota", "Camry", 2021, "White", FuelType.PETROL, "12212")
        );

        when(carService.searchCars(
                eq("Toyota"),
                eq("Camry"),
                eq(2020),
                eq("Black"),
                eq(FuelType.PETROL),
                eq("year"),
                eq(false),
                eq(0),
                eq(10)))
                .thenReturn(cars);

        given()
                .queryParam("brand", "Toyota")
                .queryParam("model", "Camry")
                .queryParam("year", 2020)
                .queryParam("color", "Black")
                .queryParam("fuelType", "PETROL")
                .queryParam("sortBy", "year")
                .queryParam("asc", false)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/cars/search")
                .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].model", equalTo("Camry"))
                .body("[1].model", equalTo("Camry"));
        verify(carService, times(1)).searchCars("Toyota", "Camry", 2020, "Black", FuelType.PETROL, "year", false, 0, 10);
    }


    @Test
    void testFindByBrandAndModel_Success() {
        List<CarDTO> cars = List.of(
                new CarDTO(1L, "Audi", "A4", 2020, "Blue", FuelType.PETROL, "12212")
        );


        PaginatedResponse<CarDTO> paginatedResponse = new PaginatedResponse<>(cars, 1, 1, 0, 20);


        when(carService.findByBrandAndModel("Audi", "A4", 0, 10)).thenReturn(paginatedResponse);

        given()
                .queryParam("brand", "Audi")
                .queryParam("model", "A4")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/cars/findByBrandAndModel")
                .then()
                .statusCode(200)
                .body("items.size()", is(1))
                .body("items[0].brand", is("Audi"))
                .body("items[0].model", is("A4"))
                .body("totalItems", is(1))
                .body("totalPages", is(1))
                .body("currentPage", is(0));

        verify(carService, times(1)).findByBrandAndModel("Audi", "A4", 0, 10);
    }



    @Test
    void testFindCarsByYearRange_Success() {
        List<CarDTO> cars = List.of(
                new CarDTO(1L, "Tesla", "Model 3", 2019, "Red", FuelType.ELECTRIC, "12212"),
                new CarDTO(2L, "BMW", "X5", 2022, "Black", FuelType.DIESEL, "12212")
        );

        PaginatedResponse<CarDTO> paginatedResponse = new PaginatedResponse<>(cars, 1, 1, 0, 20);

        when(carService.findCarsByYearRange(2018, 2022, 0, 10)).thenReturn(paginatedResponse);

        given()
                .queryParam("startYear", 2018)
                .queryParam("endYear", 2022)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/cars/findByYearRange")
                .then()
                .statusCode(200)
                .body("items.size()", is(2))
                .body("items[0].brand", is("Tesla"))
                .body("items[1].brand", is("BMW"))
                .body("totalItems", is(1))
                .body("totalPages", is(1))
                .body("currentPage", is(0));

        verify(carService, times(1)).findCarsByYearRange(2018, 2022, 0, 10);
    }


    @Test
    void testFindCarsByYearRange_InvalidYearRange() {
        given()
                .queryParam("startYear", 2023)
                .queryParam("endYear", 2020)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/cars/findByYearRange")
                .then()
                .statusCode(400)
                .contentType(ContentType.TEXT)
                .body(equalTo("Start year must be less than or equal to end year"));
    }


}
