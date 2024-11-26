package com.carsoffer.car.controller;

import com.carsoffer.PostgreSQLResource;
import com.carsoffer.car.dto.FuelType;
import com.carsoffer.car.entity.Car;
import com.carsoffer.car.repository.CarRepository;
import com.carsoffer.offer.repository.OfferRepository;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(PostgreSQLResource.class)
@TestHTTPEndpoint(CarController.class)
class CarControllerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(CarControllerIntegrationTest.class);

    @Inject
    CarRepository carRepository;

    @Inject
    OfferRepository offerRepository;

    private Long carId;

    private Car createTestCar() {
        Car car = new Car();
        car.setBrand("Audi");
        car.setModel("A4-" + UUID.randomUUID());
        car.setYear(2020);
        car.setColor("Blue");
        car.setFuelType(FuelType.DIESEL);
        car.setVin("VIN-" + UUID.randomUUID());
        return car;
    }


    @BeforeEach
    @Transactional
    void setup() {
        offerRepository.deleteAll();
        carRepository.deleteAll();
        Car car = createTestCar();
        carRepository.persist(car);
        carId = car.getId();
    }

    @Test
    public void testGetAllCars() {
        given()
                .queryParam("page", 0)
                .queryParam("pageSize", 10)
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("items", notNullValue())
                .body("items.size()", greaterThanOrEqualTo(1))
                .body("items[0].brand", equalTo("Audi"));
    }

    @Test
    public void testCreateCar() {
        String jsonBody = String.format("""
                {
                    "brand": "%s",
                    "model": "%s",
                    "year": %d,
                    "color": "%s",
                    "fuelType": "%s",
                    "vin": "%s"
                }
                """,
                "Mercedes",
                "C-Class-" + UUID.randomUUID(),
                2021,
                "White",
                FuelType.PETROL,
                "AQW123123HGF123D4"
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/")
                .then().log().all()
                .statusCode(201)
                .body("message", equalTo("Car successfully created"))
                .body("car.brand", equalTo("Mercedes"))
                .body("car.model", containsString("C-Class"))
                .extract()
                .response();

        Integer createdCarId = response.path("car.id");
        assertNotNull(createdCarId, "Car ID should not be null after creation");
    }

    @Test
    public void testGetCarById() {
        Response response = given()
                .pathParam("id", carId)
                .when()
                .get("/{id}")
                .then()
                .statusCode(200)
                .body("brand", equalTo("Audi"))
                .extract()
                .response();

        String brand = response.path("brand");
        assertNotNull(brand, "Brand should not be null");
    }


    @Test
    public void testGetCarByIdWithOffers() {
        String carJson = String.format("""
        {
            "brand": "%s",
            "model": "%s",
            "color": "%s",
            "fuelType": "%s",
            "year": %d,
            "vin": "%s"
        }
        """,
                "Audi",
                "A4",
                "Blue",
                "DIESEL",
                2020,
                "AQWE112231FDS2123"
        );

        Response carResponse = given()
                .contentType(ContentType.JSON)
                .body(carJson)
                .when()
                .post("/")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .response();

        String offerJson = String.format("""
        {
            "customerFirstName": "%s",
            "customerLastName": "%s",
            "price": %.2f,
            "carId": %d
        }
        """,
                "Alice",
                "Smith",
                300.00,
                carId
        );

        Response offerResponse = given()
                .baseUri("http://localhost:8081")
                .basePath("/offers/")
                .contentType(ContentType.JSON)
                .body(offerJson)
                .when()
                .post()
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .response();
        given()
                .pathParam("id", carId)
                .when()
                .get("/{id}/with-offers")
                .then()
                .log().all()
                .statusCode(200)
                .body("brand", equalTo("Audi"))
                .body("offers", notNullValue())
                .body("offers.size()", greaterThanOrEqualTo(1))
                .body("offers[0].customerFirstName", equalTo("Alice"));
    }

    @Test
    public void testUpdateCar() {
        String updatedJsonBody = String.format("""
                {
                    "brand": "%s",
                    "model": "%s",
                    "year": %d,
                    "color": "%s",
                    "fuelType": "%s",
                    "vin": "%s"
                }
                """,
                "Audi",
                "A6-" + UUID.randomUUID(),
                2022,
                "Silver",
                FuelType.ELECTRIC,
                "AQW123123HGF123D4"
        );

        given()
                .contentType(ContentType.JSON)
                .body(updatedJsonBody)
                .pathParam("id", carId)
                .when()
                .put("/{id}")
                .then()
                .statusCode(200)
                .body("message", equalTo("Car successfully Updated"))
                .body("car.model", containsString("A6"))
                .body("car.color", equalTo("Silver"))
                .body("car.fuelType", equalTo("ELECTRIC"));
    }

    @Test
    public void testDeleteCar() {
        Long deleteCarId = carId;

        given()
                .pathParam("id", deleteCarId)
                .when()
                .delete("/{id}")
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("Successfully deleted car!"));

        given()
                .pathParam("id", deleteCarId)
                .when()
                .get("/{id}")
                .then()
                .statusCode(404);
    }



    @Test
    public void testSearchCars() {


        given()
                .queryParam("brand", "Audi")
                .queryParam("model", "A4")
                .queryParam("year", 2020)
                .queryParam("color", "Blue")
                .queryParam("fuelType", "DIESEL")
                .queryParam("sortBy", "year")
                .queryParam("asc", true)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].brand", equalTo("Audi"))
                .body("[0].model", containsString("A4"))
                .body("[0].year", equalTo(2020))
                .body("[0].color", equalTo("Blue"))
                .body("[0].fuelType", equalTo("DIESEL"));
    }

    @Test
    @Transactional
    public void testFindByBrandAndModel() {
        Car car1 = createTestCar();
        car1.setBrand("Audi");
        car1.setModel("A4-" + UUID.randomUUID());
        carRepository.persist(car1);

        Car car2 = createTestCar();
        car2.setBrand("Audi");
        car2.setModel("A4-" + UUID.randomUUID());
        carRepository.persist(car2);

        given()
                .queryParam("brand", "Audi")
                .queryParam("model", "A4")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/findByBrandAndModel")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", greaterThanOrEqualTo(1))
                .body("items[0].brand", equalTo("Audi"))
                .body("items[0].model", containsString("A4"));
    }

    @Test
    @Transactional
    public void testFindCarsByYearRange() {
        Car car1 = createTestCar();
        car1.setYear(2019);
        carRepository.persist(car1);

        Car car2 = createTestCar();
        car2.setYear(2021);
        carRepository.persist(car2);

        given()
                .queryParam("startYear", 2019)
                .queryParam("endYear", 2021)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/findByYearRange")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.size()", greaterThanOrEqualTo(1))
                .body("items[0].year", greaterThanOrEqualTo(2019))
                .body("items[0].year", lessThanOrEqualTo(2021));
    }

    @Test
    public void testGetAllCars_withPageSizeTooLarge() {
        given()
                .queryParam("page", 0)
                .queryParam("pageSize", 101)
                .when()
                .get("/")
                .then().log().all()
                .statusCode(400)
                .body("details", equalTo("Page size too large. Maximum is 100"));
    }

    @Test
    public void testFindCarsByYearRange_withInvalidRange() {
        given()
                .queryParam("startYear", 2022)
                .queryParam("endYear", 2020)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/findByYearRange")
                .then().log().all()
                .statusCode(400)
                .contentType(ContentType.TEXT)
                .body(org.hamcrest.Matchers.equalTo("Start year must be less than or equal to end year"));
    }

    @Test
    public void testSearchCars_noResults() {
        given()
                .queryParam("brand", "NonExistentBrand")
                .queryParam("model", "NonExistentModel")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/search")
                .then().log().all()
                .statusCode(200)
                //TODO promjenit da ocekujem odgovor da ne postoji rezults s tim pararmetrima umjesto prazne liste
                .body(is("[]"));
    }

    @Test
    public void testCreateCar_withInvalidData() {
        String invalidJsonBody = """
                {
                    "brand": "",
                    "model": "",
                    "year": -2020,
                    "color": "",
                    "fuelType": "INVALID_FUEL",
                    "vin": ""
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidJsonBody)
                .when()
                .post("/")
                .then().log().all()
                .statusCode(400)
                .body("error", equalTo("Invalid fuel type"))
                .body("details", containsString("Invalid fuel type: INVALID_FUEL. Allowed values are"));
    }

    @Test
    public void testUpdateCar_NotFound() {
        Long nonExistingId = 99999L;
        String updatedJsonBody = String.format("""
                {
                    "brand": "%s",
                    "model": "%s",
                    "year": %d,
                    "color": "%s",
                    "fuelType": "%s",
                    "vin": "%s"
                }
                """,
                "Mercedes",
                "E-Class-" + UUID.randomUUID(),
                2023,
                "Gray",
                FuelType.HYBRID,
                "VIN-" + UUID.randomUUID()
        );

        given()
                .contentType(ContentType.JSON)
                .body(updatedJsonBody)
                .pathParam("id", nonExistingId)
                .when()
                .put("/{id}")
                .then()
                .statusCode(404)
                .body("message", equalTo("Car not found"));
    }

    @Test
    public void testDeleteCar_NotFound() {
        Long nonExistingId = 99999L;

        given()
                .pathParam("id", nonExistingId)
                .when()
                .delete("/{id}")
                .then()
                .statusCode(404)
                .body("message", equalTo("Entity not found"));
    }

    @Test
    public void testUpdateCar_withInvalidData() {
        String invalidJsonBody = """
                {
                    "brand": "",
                    "model": "",
                    "year": -2020,
                    "color": "",
                    "fuelType": "INVALID_FUEL",
                    "vin": ""
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidJsonBody)
                .pathParam("id", carId)
                .when()
                .put("/{id}")
                .then()
                .statusCode(400)
                .body("error", equalTo("Invalid fuel type"))
                .body("details", containsString("Invalid fuel type: INVALID_FUEL. Allowed values are"));
    }
}