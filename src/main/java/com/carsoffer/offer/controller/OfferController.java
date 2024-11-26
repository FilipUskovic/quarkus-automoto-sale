package com.carsoffer.offer.controller;

import com.carsoffer.common.exceptions.dto.ErrorResponse;
import com.carsoffer.common.utils.PaginatedResponse;
import com.carsoffer.offer.dto.CreateOfferDTO;
import com.carsoffer.offer.dto.OfferDTO;
import com.carsoffer.offer.dto.OfferSearchCriteria;
import com.carsoffer.offer.dto.UpdateOfferDTO;
import com.carsoffer.offer.service.OfferServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.carsoffer.common.utils.DateParser.parseDate;

@Path("/offers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OfferController {

    private OfferServiceImpl offerService;

    @Inject
    public OfferController(OfferServiceImpl offerService){
        this.offerService = offerService;
    }

    @GET
    @Path("/")
    public Response getAllOffers(@QueryParam("page") @DefaultValue("0")  @Min(0) int page,
                                                    @QueryParam("size") @DefaultValue("10")  @Min(1) int size) {
       validatePageSize(size);
        PaginatedResponse<OfferDTO> response = offerService.getAllOffer(page, size);
        if (response.getItems().isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Nema dostupnih ponuda.",
                    "Nema ponuda dostupnih za traženi upit."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }

        return  Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    public Response getOfferById(@PathParam("id") Long id) {
        OfferDTO offerDTO = offerService.findOfferById(id);
        if (offerDTO == null) {
            ErrorResponse errorResponse = new ErrorResponse(
                    String.format("Ponuda s ID-om %d nije pronađena.", id),
                    "Molimo provjerite uneseni ID."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }
        return Response.ok(offerDTO).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleterOffer(@PathParam("id") Long id) {
        offerService.deleteOffer(id);
        Map<String, String> responseMessage = Map.of("message", "Successfully deleted offer!");
        return Response.ok(responseMessage).build();
    }



    @Operation(summary = "Create a new offer", description = "Creates a new offer in the system and returns the created offer.")
    @APIResponse(responseCode = "201", description = "Offer created successfully", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Invalid input data", content = @Content(mediaType = "application/json"))
    @POST
    public Response createOffer(@Valid @RequestBody CreateOfferDTO createOfferDTO) {
        OfferDTO createdOffer = offerService.createOffer(createOfferDTO);
        Map<String, Object> response = Map.of(
                "message", "Offer successfully created",
                "offer", createdOffer
        );
        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response updateOffer(@PathParam("id") Long id, @Valid @RequestBody UpdateOfferDTO updateOfferDTO) {
        OfferDTO updatedOffer = offerService.updateOffer(id, updateOfferDTO);
        Map<String, Object> response = Map.of(
                "message", "Offer successfully Updated",
                "offer", updatedOffer
        );
        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }


    @GET
    @Path("/search")
    @Operation(summary = "Search offers with optional sorting and pagination")
    public Response searchOffers(
            @QueryParam("customerFirstName") String customerFirstName,
            @QueryParam("customerLastName") String customerLastName,
            @QueryParam("minPrice") Double minPrice,
            @QueryParam("maxPrice") Double maxPrice,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @Parameter(description = "Field to sort by, default is 'id'")
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("asc") @DefaultValue("true") boolean asc,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {

        LocalDate parsedStartDate = parseDate(startDate, "Start date");
        LocalDate parsedEndDate = parseDate(endDate, "End date");

        if (parsedStartDate != null && parsedEndDate != null && parsedStartDate.isAfter(parsedEndDate)) {
            return buildErrorResponse();
        }

        OfferSearchCriteria criteria = new OfferSearchCriteria(
                customerFirstName, customerLastName, minPrice, maxPrice, parsedStartDate,
                parsedEndDate, sortBy, asc, page, size
        );
        List<OfferDTO> offers = offerService.searchOffers(criteria);
        if (offers.isEmpty()) {
            Map<String, String> responseMessage = Map.of("message", "No offers found for the given search criteria.");
            return Response.status(Response.Status.NOT_FOUND).entity(responseMessage).build();
        }
        return Response.ok(offers).build();
    }


    @GET
    @Path("/findByCustomerNames")
    public Response findCustomerByFirstAndLastName(@QueryParam("firstName") String firstName, @QueryParam("lastName") String lastName,
                                        @QueryParam("page") @DefaultValue("0")  @Min(0) int page,
                                        @QueryParam("size") @DefaultValue("10") @Min(1)  int size) {
        validatePageSize(size);
        validateCustomerName(firstName, "firstName");
        validateCustomerName(lastName, "lastName");
        PaginatedResponse<OfferDTO> offers = offerService.getOffersByCustomerName(firstName, lastName, page, size);
        if (offers.getItems().isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(
                    String.format("Nema ponuda za kupca s imenom '%s' i prezimenom '%s'", firstName, lastName),
                    "Molimo provjerite unesene parametre."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }
        return Response.ok(offers).build();
    }

    @GET
    @Path("/findByPricesBetween")
    public Response offersByPriceRange(@QueryParam("minPrice") BigDecimal minPrice,@QueryParam("maxPrice") BigDecimal maxPrice,
                                                   @QueryParam("page") @DefaultValue("0")  @Min(0) int page,
                                                   @QueryParam("size") @DefaultValue("10") @Min(1)  int size) {
        validatePageSize(size);
        PaginatedResponse<OfferDTO> offers = offerService.getOffersByPriceRange(minPrice, maxPrice, page, size);

        if (offers.getItems().isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(
                    String.format("Nema ponuda između cijena %s i %s.", minPrice, maxPrice),
                    "Molimo provjerite unesene parametre pretrage."
            );
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }
        return Response.ok(offers).build();
    }



    private void validatePageSize(int pageSize) {
        if (pageSize > 100) {
            throw new IllegalArgumentException("Page size too large. Maximum is 100");
        }
    }


    private void validateCustomerName(String name, String nameType) {
        if (name != null && !name.matches("[a-žA-Ž]+")) {
            throw new IllegalArgumentException(nameType + " can only contain alphabetic characters.");
        }
    }

    private Response buildErrorResponse() {
        Map<String, String> responseMessage = Map.of("message", "Start date cannot be after end date.");
        return Response.status(Response.Status.BAD_REQUEST).entity(responseMessage).build();
    }

}
