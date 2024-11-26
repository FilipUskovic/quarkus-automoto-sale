package com.carsoffer.car.controller;

import com.carsoffer.car.dto.*;
import com.carsoffer.car.service.CarServiceImpl;
import com.carsoffer.common.exceptions.dto.ErrorResponse;
import com.carsoffer.common.utils.PaginatedResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Path("/cars")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarController {

    private static final Logger log = LoggerFactory.getLogger(CarController.class);
    private final CarServiceImpl carService;

    @Inject
    public CarController(CarServiceImpl carService) {
        this.carService = carService;
    }

    @GET
    @Path("/")
    public Response getAllCars(@QueryParam("page") @DefaultValue("0") @Min(0) int page
                              ,@QueryParam("pageSize") @DefaultValue("20") @Min(1) int pageSize) {
        validatePageSize(pageSize);
        PaginatedResponse<CarDTO> response = carService.getAllCars(page, pageSize);
        if (response.getItems().isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Nema rezultata.",
                    "Nema automobila dostupnih za traženi upit."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getCarById(@PathParam("id") Long id) {
        CarDTO carDTO = carService.getCarById(id);
        if (carDTO == null) {
            ErrorResponse errorResponse = new ErrorResponse(
                    String.format("Automobil s ID-om %d nije pronađen.", id),
                    "Molimo provjerite uneseni ID."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }
        return Response.ok(carDTO).build();
    }

    @GET
    @Path("/{id}/with-offers")
    public Response getCarByIdWithOffers(@PathParam("id") Long id) {
        CarWithOfferDTO carWithOffers = carService.getCarByIdWithOffers(id);
        if (carWithOffers == null) {
            ErrorResponse errorResponse = new ErrorResponse(
                    String.format("Automobil s ID-om %d i njegovim ponudama nije pronađen.", id),
                    "Molimo provjerite uneseni ID."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }
        return Response.ok(carWithOffers).build();
    }

    @Operation(summary = "Create a new car", description = "Creates a new car in the system and returns the created car.")
    @APIResponse(responseCode = "201", description = "Car created successfully", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Invalid input data", content = @Content(mediaType = "application/json"))
    @POST
    public Response createCar(@Valid @RequestBody CreateCarDTO createCarDTO) {
        CarDTO createdCar = carService.createCar(createCarDTO);
        log.info("Created car DTO before sending response: {}", createdCar);

        Map<String, Object> response = Map.of(
                "message", "Car successfully created",
                "car", createdCar
        );
        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response updateCar(@PathParam("id") Long id, @Valid @RequestBody UpdateCarDTO carDTO) {
        CarDTO updatedCar = carService.updateCar(id, carDTO);
        Map<String, Object> response = Map.of(
                "message", "Car successfully Updated",
                "car", updatedCar
        );
        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCar(@PathParam("id") Long id) {
        carService.deleteCar(id);
        Map<String, String> responseMessage = Map.of("message", "Successfully deleted car!");
        return Response.ok(responseMessage).build();
    }

    @GET
    @Path("/search")
    @Operation(summary = "Search cars with optional sorting and pagination")
    public Response searchCars(
            @QueryParam("brand") String brand,
            @QueryParam("model") String model,
            @QueryParam("year") Integer year,
            @QueryParam("color") String color,
            @QueryParam("fuelType") FuelType fuelType,
            @Parameter(description = "Field to sort by, default is 'id'")
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,

            @Parameter(description = "Ascending order if true, descending if false")
            @QueryParam("asc") @DefaultValue("true") boolean asc,

            @Parameter(description = "Page number for pagination, default is 0")
            @QueryParam("page") @DefaultValue("0") int page,

            @Parameter(description = "Number of items per page, default is 10")
            @QueryParam("size") @DefaultValue("10") int size
    ) {
        validatePageSize(size);
        List<CarDTO> cars = carService.searchCars(brand, model, year, color, fuelType, sortBy, asc, page, size);
        return Response.ok(cars).build();
    }

    @GET
    @Path("/findByBrandAndModel")
    public Response findByBrandAndModel(@QueryParam("brand") String brand, @QueryParam("model") String model,
                                        @QueryParam("page") @DefaultValue("0")  @Min(0) int page,
                                        @QueryParam("size") @DefaultValue("10") @Min(1)  int size) {
        validatePageSize(size);
        PaginatedResponse<CarDTO> cars = carService.findByBrandAndModel(brand, model, page, size);

        if (cars.getItems().isEmpty()) {
            String modelDisplay = model != null ? model : "null";
            ErrorResponse errorResponse = new ErrorResponse(
                    String.format("Nema rezultata za brand '%s' i model '%s'", brand, modelDisplay),
                    "Molimo provjerite unesene parametre."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }

        return Response.ok(cars).build();
    }

    @GET
    @Path("/findByYearRange")
    public Response findCarsByYearRange(@QueryParam("startYear") int startYear, @QueryParam("endYear") int endYear,
                                        @QueryParam("page") @DefaultValue("0") @Min(0) int page,
                                        @QueryParam("size") @DefaultValue("10") @Min(1) int size) {
        if (startYear > endYear) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Start year must be less than or equal to end year")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        PaginatedResponse<CarDTO> cars = carService.findCarsByYearRange(startYear, endYear, page, size);
        if (cars.getItems().isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(
                    String.format("Nema rezultata za godine između %d i %d.", startYear, endYear),
                    "Molimo provjerite unesene godine."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }
        return Response.ok(cars).build();
    }


    private void validatePageSize(int pageSize) {
        if (pageSize > 100) {
            throw new IllegalArgumentException("Page size too large. Maximum is 100");
        }
    }


}
