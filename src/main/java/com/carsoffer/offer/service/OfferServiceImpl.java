package com.carsoffer.offer.service;

import com.carsoffer.car.entity.Car;
import com.carsoffer.car.repository.CarRepository;
import com.carsoffer.common.exceptions.CarNotFoundException;
import com.carsoffer.common.exceptions.OfferNotFoundException;
import com.carsoffer.common.mappers.OfferMapper;
import com.carsoffer.common.utils.PaginatedResponse;
import com.carsoffer.offer.dto.CreateOfferDTO;
import com.carsoffer.offer.dto.OfferDTO;
import com.carsoffer.offer.dto.OfferSearchCriteria;
import com.carsoffer.offer.dto.UpdateOfferDTO;
import com.carsoffer.offer.entity.Offer;
import com.carsoffer.offer.repository.OfferRepository;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class OfferServiceImpl implements OfferService {

    private static final Logger log = LoggerFactory.getLogger(OfferServiceImpl.class);
    private final OfferRepository offerRepository;
    private final CarRepository carRepository;


    @Inject
    public OfferServiceImpl(OfferRepository offerRepository, CarRepository carRepository) {
        this.offerRepository = offerRepository;
        this.carRepository = carRepository;
    }


    @Override
    @CacheResult(cacheName = "offer-list-cache")
    public PaginatedResponse<OfferDTO> getAllOffer(int page, int size) {
        log.info("Fetching all offers - page {}, size {}", page, size);
        PanacheQuery<Offer> offersQuery = offerRepository.findAllPaged(page, size);

        List<OfferDTO> offerDTOS = offersQuery.stream()
                .map(OfferMapper::toDTO).toList();

        long totalItems = offersQuery.count();
        return new PaginatedResponse<>(
                    offerDTOS,
                    totalItems,
                   offersQuery.pageCount(),
                  page, size
        );
    }

    @Override
    @CacheResult(cacheName = "offer-cache")
    public OfferDTO findOfferById(Long id) {
        log.info("Fetching offer by ID: {}", id);
        return offerRepository.findByIdOptional(id)
                .map(OfferMapper::toDTO)
                .orElseThrow(() -> new OfferNotFoundException(id));
    }

    @Override
    @Transactional
    @CacheInvalidateAll(cacheName = "offer-cache")
    @CacheInvalidateAll(cacheName = "offer-list-cache")
    public OfferDTO createOffer(CreateOfferDTO createOfferDTO) {
        log.info("Creating new offer with details: {}", createOfferDTO);
        Car car = carRepository.findByIdOptional(createOfferDTO.carId())
                .orElseThrow(() -> new CarNotFoundException(createOfferDTO.carId()));

        Offer offer = new Offer.Builder()
                .customerFirstName(createOfferDTO.customerFirstName())
                .customerLastName(createOfferDTO.customerLastName())
                .price(createOfferDTO.price())
                .car(car)
                .offerDate(LocalDateTime.now())
                .lastModifiedDate(null)
                .build();

        offerRepository.persist(offer);
        return OfferMapper.toDTO(offer);
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = "offer-cache")
    @CacheInvalidateAll(cacheName = "offer-list-cache")
    public OfferDTO updateOffer(@CacheKey Long id, UpdateOfferDTO offerDTO) {
        log.info("Updating offer ID: {}", id);

        Offer offer = offerRepository.findOfferWithCarById(id)
                .orElseThrow(() ->  new OfferNotFoundException(id));

        offer.setCustomerFirstName(offerDTO.customerFirstName());
        offer.setCustomerLastName(offerDTO.customerLastName());
        offer.setPrice(offerDTO.price());

        Car currentCar = offer.getCar();
        if (currentCar == null || !currentCar.getId().equals(offerDTO.carId())) {
            Car car = carRepository.findByIdOptional(offerDTO.carId())
                    .orElseThrow(() -> new CarNotFoundException(offerDTO.carId()));
            offer.setCar(car);
        }

        return OfferMapper.toDTO(offer);
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = "offer-cache")
    @CacheInvalidateAll(cacheName = "offer-list-cache")
    public void deleteOffer(@CacheKey Long id) {
        log.info("Deleting offer ID: {}", id);
        Offer offer = offerRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with ID " + id + " not found"));
        offerRepository.delete(offer);
    }

    @Override
    public PaginatedResponse<OfferDTO> getOffersByCustomerName(String firstName, String lastName, int page, int size) {
        if ((firstName == null || firstName.isBlank()) && (lastName == null || lastName.isBlank())) {
            throw new IllegalArgumentException("firstName or lastName must be provided.");
        }
        PanacheQuery<Offer> offerQuery = offerRepository.findByCustomerByFirstNameAndCustomerByLastNamePaged(firstName, lastName, page, size);
        long totalItems = offerQuery.count();
        List<OfferDTO> offerDTOS = offerQuery.stream()
                .map(OfferMapper::toDTO).toList();

        return new PaginatedResponse<>(offerDTOS, totalItems, offerQuery.pageCount(), page, size);
    }

    @Override
    public PaginatedResponse<OfferDTO> getOffersByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
       validatePrices(minPrice, maxPrice);
        PanacheQuery<Offer> offerQuery = offerRepository.findByPriceBetweenPaged(minPrice, maxPrice, page, size);
        long totalItems = offerQuery.count();

        List<OfferDTO> offerDTOs = offerQuery
                .stream().map(OfferMapper::toDTO).toList();
        return new PaginatedResponse<>(offerDTOs, totalItems, offerQuery.pageCount(), page, size);
    }

    @Override
    public List<OfferDTO> searchOffers(OfferSearchCriteria criteria) {
        log.info("Searching offers with criteria: {}", criteria);
        if ((criteria.customerFirstName() == null || criteria.customerFirstName().isBlank()) &&
                (criteria.customerLastName() == null || criteria.customerLastName().isBlank()) &&
                criteria.minPrice() == null && criteria.maxPrice() == null &&
                criteria.startDate() == null && criteria.endDate() == null) {
            throw new IllegalArgumentException("At least one search parameter must be provided.");
        }

        List<Offer> offers = offerRepository.searchOffers(criteria);
        log.info("Number of offers found: {}", offers.size());

        return offers.stream()
                .map(OfferMapper::toDTO)
                .toList();
    }

    //TODO kreirati ponudu s autom aka cijelim objektom audta (OptiMALNO)


    public void validatePrices(BigDecimal minPrice, BigDecimal maxPrice) {
        log.info("Validating prices: minPrice={}, maxPrice={}", minPrice, maxPrice);
        if (minPrice == null || maxPrice == null) {
            throw new IllegalArgumentException("Prices cannot be null.");
        }

        if (minPrice.compareTo(BigDecimal.ZERO) <= 0 || maxPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Both minPrice and maxPrice must be positive numbers.");
        }

        if (minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must be less than or equal to maxPrice.");
        }
    }
}
