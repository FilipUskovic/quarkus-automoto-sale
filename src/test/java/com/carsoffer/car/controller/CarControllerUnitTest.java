package com.carsoffer.car.controller;

import com.carsoffer.car.dto.CarDTO;
import com.carsoffer.car.dto.CreateCarDTO;
import com.carsoffer.car.dto.FuelType;
import com.carsoffer.car.dto.UpdateCarDTO;
import com.carsoffer.car.service.CarServiceImpl;
import com.carsoffer.common.exceptions.CarNotFoundException;
import com.carsoffer.common.exceptions.GlobalExceptionHandler;
import com.carsoffer.common.exceptions.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarControllerUnitTest {

    @Mock
    private CarServiceImpl carService;

    @InjectMocks
    private CarController carController;

    @Test
    void testCreateCar_Success() {
        CreateCarDTO createCarDTO = new CreateCarDTO("BMW", "X5", 2022, "Black", FuelType.PETROL, "123qwe123qwe123qw");

        when(carService.createCar(any(CreateCarDTO.class))).thenReturn(new CarDTO(1L, "BMW", "X5", 2022, "Black", FuelType.PETROL, "123qwe123qwe123qw"));
        Response response = carController.createCar(createCarDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        Map<String, Object> responseBody = (Map<String, Object>) response.getEntity();
        CarDTO returnedCar = (CarDTO) responseBody.get("car");
        assertEquals("BMW", returnedCar.brand());
        assertEquals("X5", returnedCar.model());

        verify(carService, times(1)).createCar(any(CreateCarDTO.class));
    }





    @Test
    void testGetCarById_Success() {
        CarDTO carDTO = new CarDTO(1L, "Audi", "A4", 2020, "Blue", FuelType.PETROL, "123qwe123qwe123qw");
        when(carService.getCarById(1L)).thenReturn(carDTO);
        Response response = carController.getCarById(1L);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        CarDTO returnedCar = (CarDTO) response.getEntity();
        assertEquals("Audi", returnedCar.brand());
        assertEquals("A4", returnedCar.model());

        verify(carService, times(1)).getCarById(1L);
    }

    @Test
    void testGetCarById_NotFound() {
        when(carService.getCarById(999L)).thenThrow(new CarNotFoundException(999L));
        try {
            carController.getCarById(999L);
            fail("Expected CarNotFoundException to be thrown");
        } catch (CarNotFoundException e) {
            Response response = handleException(e);

            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertTrue(response.getEntity() instanceof ErrorResponse);

            ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
            assertEquals("Car not found", errorResponse.getMessage());
            assertEquals("Car with ID 999 not found", errorResponse.getDetails());

            verify(carService, times(1)).getCarById(999L);
        }
    }

    @Test
    void testDeleteCar_Success() {
        doNothing().when(carService).deleteCar(1L);
        Response response = carController.deleteCar(1L);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        verify(carService, times(1)).deleteCar(1L);
    }

    @Test
    void testDeleteCar_NotFound() {
        doThrow(new CarNotFoundException(999L)).when(carService).deleteCar(999L);

        try {
            carController.deleteCar(999L);
            fail("Expected CarNotFoundException to be thrown");
        } catch (CarNotFoundException e) {
            Response response = handleException(e);

            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertTrue(response.getEntity() instanceof ErrorResponse);

            ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
            assertEquals("Car not found", errorResponse.getMessage());
            assertEquals("Car with ID 999 not found", errorResponse.getDetails());

            verify(carService, times(1)).deleteCar(999L);
        }
    }




    @Test
    void testUpdateCar_Success() {
        UpdateCarDTO updateCarDTO = new UpdateCarDTO("BMW", "X7", 2023, "White", FuelType.DIESEL);
        CarDTO updatedCarDTO = new CarDTO(1L, "BMW", "X7", 2023, "White", FuelType.DIESEL, "123qwe123qwe123qw");
        when(carService.updateCar(eq(1L), any(UpdateCarDTO.class))).thenReturn(updatedCarDTO);

        Response response = carController.updateCar(1L, updateCarDTO);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Map<String, Object> responseBody = (Map<String, Object>) response.getEntity();
        assertTrue(responseBody.containsKey("car"));

        CarDTO returnedCar = (CarDTO) responseBody.get("car");

        assertEquals("BMW", returnedCar.brand());
        assertEquals("X7", returnedCar.model());
        assertEquals(2023, returnedCar.year());
        assertEquals("White", returnedCar.color());
        assertEquals(FuelType.DIESEL, returnedCar.fuelType());
        assertEquals("123qwe123qwe123qw", returnedCar.vin());

        verify(carService, times(1)).updateCar(eq(1L), any(UpdateCarDTO.class));
    }



    private Response handleException(Throwable exception) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        return handler.toResponse(exception);
    }

}
