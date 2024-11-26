package com.carsoffer.offer.service;

import com.carsoffer.common.utils.PaginatedResponse;
import com.carsoffer.offer.dto.CreateOfferDTO;
import com.carsoffer.offer.dto.OfferDTO;
import com.carsoffer.offer.dto.OfferSearchCriteria;
import com.carsoffer.offer.dto.UpdateOfferDTO;

import java.math.BigDecimal;
import java.util.List;

public interface OfferService {

    PaginatedResponse<OfferDTO> getAllOffer(int page, int size);

    OfferDTO findOfferById(Long id);

    OfferDTO createOffer(CreateOfferDTO createOfferDTO);

    OfferDTO updateOffer(Long id, UpdateOfferDTO updateOfferDTO);

    void deleteOffer(Long id);

    PaginatedResponse<OfferDTO> getOffersByCustomerName(String firstName, String lastName, int page, int size);

    PaginatedResponse<OfferDTO> getOffersByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size);

    List<OfferDTO> searchOffers(OfferSearchCriteria offerSearchCriteria);


}
