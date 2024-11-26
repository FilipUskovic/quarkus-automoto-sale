package com.carsoffer.offer.controller;

import com.carsoffer.PostgreSQLResource;
import com.carsoffer.car.dto.FuelType;
import com.carsoffer.car.entity.Car;
import com.carsoffer.car.repository.CarRepository;
import com.carsoffer.offer.entity.Offer;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(PostgreSQLResource.class)
@TestHTTPEndpoint(OfferController.class)
class OfferControllerIntegrationTest {


    @Inject
    CarRepository carRepository;

    @Inject
    OfferRepository offerRepository;

    private Long carId;
    private Long offerId;

    private Car createTestCar() {
        return new Car.Builder()
                .brand("BMW")
                .model("M6-" + UUID.randomUUID())
                .color("Black")
                .year(2006)
                .fuelType(FuelType.HYBRID)
                .vin("VIN-" + UUID.randomUUID())
                .build();
    }


    private Offer createTestOffer(Car car) {
        return new Offer.Builder()
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(500.00))
                .offerDate(LocalDate.now().minusDays(1).atStartOfDay())
                .lastModifiedDate(LocalDate.now().plusDays(1).atStartOfDay())
                .car(car)
                .build();
    }

    @BeforeEach
    @Transactional
    void setup() {
        offerRepository.deleteAll();
        carRepository.deleteAll();
        Car car = createTestCar();
        carRepository.persist(car);
        Offer offer = createTestOffer(car);
        offerRepository.persist(offer);
        carId = car.getId();
        offerId = offer.getId();
    }



    @Test
    public void testGetAllOffers() {
        String jsonBody = String.format("""
                {
                    "customerFirstName": "%s",
                    "customerLastName": "%s",
                    "price": %s,
                    "carId": %d
                }
                """, "Luka", "Borna", 200.00, carId);

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/")
                .then()
                .statusCode(201);

        given()
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("items", notNullValue())
                .body("items.size()", greaterThanOrEqualTo(1))
                .body("items[0].customerFirstName", equalTo("Luka"));
    }


    @Test
    public void testCreateOffer() {
        Long carId1 = carId;
        String jsonBody = String.format("""
            {
                "customerFirstName": "%s",
                "customerLastName": "%s",
                "price": %s,
                "carId": %d
            }
            """, "Luka", "Borna", 200.00, carId1);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/");

        System.out.println("Response Body: " + response.asString());

        response.then()
                .statusCode(201)
                .body("offer.customerFirstName", equalTo("Luka"))
                .body("offer.customerLastName", equalTo("Borna"))
                .body("offer.price", equalTo(200.00f))
                .body("offer.carId", equalTo(carId.intValue()));
    }


    @Test
    public void testGetOfferById() {
        String jsonBody = String.format("""
        {
            "customerFirstName": "%s",
            "customerLastName": "%s",
            "price": %s,
            "carId": %d
        }
        """, "Luka", "Borna", 200.00, carId);

        Response postResponse = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Integer offerIdInt = postResponse.path("offer.id");
        System.out.println("Created Offer ID: " + offerIdInt.longValue());


        given()
                .pathParam("id", offerIdInt.longValue())
                .when()
                .get("/{id}")
                .then()
                .statusCode(200)
                .body("customerFirstName", equalTo("Luka"))
                .body("customerLastName", equalTo("Borna"))
                .body("price", equalTo(200.00f));
    }


    @Test
    public void testInvalidPageSize() {
        given()
                .queryParam("page", -1)
                .queryParam("size", 0)
                .when()
                .get("/")
                .then()
                .statusCode(400);
    }

    @Test
    public void testDeleteOffer() {
        String jsonBody = String.format("""
    {
        "customerFirstName": "%s",
        "customerLastName": "%s",
        "price": %s,
        "carId": %d
    }
    """, "Luka", "Borna", 200.00, carId);

        Response postResponse = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Integer offerIdInt = postResponse.path("offer.id");
        System.out.println("Created Offer ID: " + offerIdInt.longValue());


        given()
                .pathParam("id", offerIdInt.longValue())
                .when()
                .delete("/{id}")
                .then()
                .statusCode(200)
                .body("message", equalTo("Successfully deleted offer!"));

        given()
                .pathParam("id", offerIdInt.longValue())
                .when()
                .get("/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testUpdateOffer() {
        String jsonBody = String.format("""
    {
        "customerFirstName": "%s",
        "customerLastName": "%s",
        "price": %s,
        "carId": %d
    }
    """, "Luka", "Borna", 200.00, carId);

        Response postResponse = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/")
                .then().log().all()
                .statusCode(201)
                .extract()
                .response();

        System.out.println("Response: " + postResponse.asString());


        Integer offerIdInt = postResponse.path("offer.id");
        System.out.println("Created Offer ID: " + offerIdInt.longValue());

        String updatedJsonBody = String.format("""
    {
        "customerFirstName": "%s",
        "customerLastName": "%s",
        "price": %s,
         "carId": %d
    }
    """, "Jane", "Borna", 250.00, carId);

        given()
                .contentType(ContentType.JSON)
                .body(updatedJsonBody)
                .pathParam("id", offerIdInt.longValue())
                .when()
                .put("/{id}")
                .then().log().all()
                .statusCode(200)
                .body("message", equalTo("Offer successfully Updated"))
                .body("offer.customerFirstName", equalTo("Jane"))
                .body("offer.price", equalTo(250.00f));
    }

    @Test
    public void testSearchOffers() {
        String jsonBody = String.format("""
    {
        "customerFirstName": "%s",
        "customerLastName": "%s",
        "price": %s,
        "carId": %d
    }
    """, "Luka", "Borna", 200.00, carId);

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/")
                .then()
                .statusCode(201);

        given()
                .queryParam("customerFirstName", "Luka")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/search")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].customerFirstName", equalTo("Luka"));
    }



    @Test
    public void testOffersByPriceRange_EmptyList() {
        given()
                .queryParam("minPrice", 200.00)
                .queryParam("maxPrice", 300.00)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/findByPricesBetween")
                .then()
                .statusCode(404)
                .body("message", equalTo("Nema ponuda između cijena 200.0 i 300.0."))
                .body("details", equalTo("Molimo provjerite unesene parametre pretrage."));
    }



    @Test
    public void testFindCustomerByFirstAndLastName(){
        String jsonBody = String.format("""
        {
            "customerFirstName": "%s",
            "customerLastName": "%s",
            "price": %s,
            "carId": %d
        }
        """, "Luka", "Borna", 200.00, carId);

        Response postResponse = given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/")
                .then().log().all()
                .statusCode(201)
                .extract()
                .response();

        Integer offerIdInt = postResponse.path("offer.id");
        assertNotNull(offerIdInt, "Offer ID should not be null after successful creation");


        given()
                .queryParam("firstName", "Luka")
                .queryParam("lastName", "Borna")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/findByCustomerNames")
                .then().log().all()
                .statusCode(200)
                .body("items.size()", greaterThanOrEqualTo(0))
                .body("items[0].customerFirstName", equalTo("Luka"))
                .body("items[0].customerLastName", equalTo("Borna"));
    }


    @Test
    public void testGetAllOffers_withPageSizeTooLarge() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 101)
                .when()
                .get("/")
                .then()
                .statusCode(400)
                .body("details", containsString("Page size too large. Maximum is 100"));
    }


    @Test
    public void testSearchOffers_withInvalidDateRange() {
        String jsonBody = String.format("""
                {
                    "customerFirstName": "%s",
                    "customerLastName": "%s",
                    "price": %s,
                    "carId": %d
                }
                """, "Alice", "Smith", 300.00, carId);

        given()
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post("/")
                .then()
                .statusCode(201);

        given()
                .queryParam("startDate", "2023-12-31")
                .queryParam("endDate", "2023-01-01")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/search")
                .then()
                .statusCode(400)
                .body("message", equalTo("Start date cannot be after end date."));
    }


    @Test
    public void testFindCustomerByFirstAndLastName_withInvalidFirstName() {
        String invalidFirstName = "John123";

        given()
                .queryParam("firstName", invalidFirstName)
                .queryParam("lastName", "Borna")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/findByCustomerNames")
                .then()
                .statusCode(400)
                .body("details", containsString("firstName can only contain alphabetic characters."));
    }

    @Test
    public void testFindCustomerByFirstAndLastName_withInvalidLastName() {
        String invalidLastName = "Borna@";

        given()
                .queryParam("firstName", "Luka")
                .queryParam("lastName", invalidLastName)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/findByCustomerNames")
                .then()
                .statusCode(400)
                .body("details", containsString("lastName can only contain alphabetic characters."));
    }

    @Test
    public void testOffersByPriceRange_withPageSizeTooLarge() {
        given()
                .queryParam("minPrice", 100.00)
                .queryParam("maxPrice", 300.00)
                .queryParam("page", 0)
                .queryParam("size", 101)
                .when()
                .get("/findByPricesBetween")
                .then()
                .statusCode(400)
                .body("details", containsString("Page size too large. Maximum is 100"));
    }

    @Test
    public void testSearchOffers_noResults() {
        given()
                .queryParam("customerFirstName", "NonExistentName")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/search")
                .then()
                .statusCode(404)
                .body("message", equalTo("No offers found for the given search criteria."));
    }

    @Test
    public void testGetOfferById_NotFound() {
        Long nonExistingId = 99999L;

        given()
                .pathParam("id", nonExistingId)
                .when()
                .get("/{id}")
                .then()
                .statusCode(404)
                .body("message", equalTo("Offer not found"));
    }

    @Test
    public void testDeleteOffer_NotFound() {
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
    public void testCreateOffer_withInvalidData() {
        String invalidJsonBody = """
                {
                    "customerFirstName": "",
                    "customerLastName": "",
                    "price": -100.00,
                    "carId": null
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidJsonBody)
                .when()
                .post("/")
                .then()
                .statusCode(400)
                .body("errors.size()", greaterThan(0));
    }

    @Test
    public void testUpdateOffer_NotFound() {
        Long nonExistingId = 99999L;

        String updatedJsonBody = String.format("""
                {
                    "customerFirstName": "%s",
                    "customerLastName": "%s",
                    "price": %s,
                    "carId": %d
                }
                """, "Jane", "Borna", 250.00, carId);

        given()
                .contentType(ContentType.JSON)
                .body(updatedJsonBody)
                .pathParam("id", nonExistingId)
                .when()
                .put("/{id}")
                .then()
                .statusCode(404)
                .body("message", equalTo("Offer not found"));
    }

    @Test
     void testSearchOffers_withValidParameters() {


        LocalDate today = LocalDate.now();
        String startDate = today.minusDays(1).toString();
        String endDate = today.plusDays(1).toString();

        given()
                .queryParam("customerFirstName", "Luka")
                .queryParam("minPrice", 400.00)
                .queryParam("maxPrice", 600.00)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .queryParam("sortBy", "price")
                .queryParam("asc", true)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/search")
                .then().log().all()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].customerFirstName", equalTo("Luka"))
                .body("[0].price", equalTo(500.00f));
    }

    @Test
    public void testOffersByPriceRange_withInvalidPriceRange() {
        given()
                .queryParam("minPrice", 300.00)
                .queryParam("maxPrice", 100.00)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/findByPricesBetween")
                .then()
                .statusCode(400)
                .body("details", equalTo("minPrice must be less than or equal to maxPrice."));
    }

}
// TODO dodaj centralizirano upravljanje za jednostavni i konzistetni nacin
/* centralizirano kreiranje json body i car i offer objekta

class TestDataFactory {
    private final CarRepository carRepository;
    private final OfferRepository offerRepository;

    TestDataFactory(CarRepository carRepository, OfferRepository offerRepository) {
        this.carRepository = carRepository;
        this.offerRepository = offerRepository;
    }

    Car createTestCar() {
        return new Car.Builder()
                .brand("BMW")
                .model("M6-" + UUID.randomUUID())
                .color("Black")
                .year(2006)
                .fuelType(FuelType.HYBRID)
                .vin("VIN-" + UUID.randomUUID())
                .build();
    }

    Car createAndPersistCar() {
        Car car = createTestCar();
        carRepository.persist(car);
        return car;
    }

    Offer createTestOffer(Car car) {
        return new Offer.Builder()
                .customerFirstName("Luka")
                .customerLastName("Borna")
                .price(BigDecimal.valueOf(500.00))
                .offerDate(LocalDate.now().minusDays(1).atStartOfDay())
                .lastModifiedDate(LocalDate.now().plusDays(1).atStartOfDay())
                .car(car)
                .build();
    }

    Offer createAndPersistOffer() {
        Car car = createAndPersistCar();
        Offer offer = createTestOffer(car);
        offerRepository.persist(offer);
        return offer;
    }

    void createMultipleOffers(int count) {
        for (int i = 0; i < count; i++) {
            createAndPersistOffer();
        }
    }

    String createOfferJson(Long carId) {
        return String.format("""
                {
                    "customerFirstName": "Luka",
                    "customerLastName": "Borna",
                    "price": 200.00,
                    "carId": %d
                }
                """, carId);
    }

    String createUpdatedOfferJson(Long carId) {
        return String.format("""
                {
                    "customerFirstName": "Jane",
                    "customerLastName": "Borna",
                    "price": 250.00,
                    "carId": %d
                }
                """, carId);
    }
}

 */