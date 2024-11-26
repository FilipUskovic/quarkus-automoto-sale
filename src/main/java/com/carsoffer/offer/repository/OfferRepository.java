package com.carsoffer.offer.repository;

import com.carsoffer.offer.dto.OfferSearchCriteria;
import com.carsoffer.offer.entity.Offer;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.math.BigDecimal;
import java.util.*;

@ApplicationScoped
public class OfferRepository implements PanacheRepository<Offer> {

    private final EntityManager entityManager;

    @Inject
    public OfferRepository(EntityManager em) {
        this.entityManager = em;
    }

    public PanacheQuery<Offer> findByCustomerByFirstNameAndCustomerByLastNamePaged(String firstName, String lastName, int page, int size) {
        String query = "select o from Offer o where 1=1";
        Map<String, Object> parameters = new HashMap<>();

        if (firstName != null && !firstName.isEmpty()) {
            query += " and lower(o.customerFirstName) like :firstName";
            parameters.put("firstName", "%" + firstName.toLowerCase() + "%");
        }

        if (lastName != null && !lastName.isEmpty()) {
            query += " and lower(o.customerLastName) like :lastName";
            parameters.put("lastName", "%" + lastName.toLowerCase() + "%");
        }

        return find(query, parameters).page(Page.of(page, size));
    }


    public PanacheQuery<Offer> findByPriceBetweenPaged(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        return find("price >= :minPrice and price <= :maxPrice",
                Parameters.with("minPrice", minPrice).and("maxPrice", maxPrice))
                .page(Page.of(page, size));
    }

    public PanacheQuery<Offer> findAllPaged(int page, int size) {
        return findAll().page(Page.of(page, size));
    }

    public Optional<Offer> findOfferWithCarById(Long offerId) {
        return find("SELECT o FROM Offer o WHERE o.id = ?1", offerId)
                .withHint("jakarta.persistence.fetchgraph", getEntityManager().getEntityGraph("Offer.car"))
                .firstResultOptional();
    }


    public List<Offer> searchOffers(OfferSearchCriteria criteria) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Offer> criteriaQuery = criteriaBuilder.createQuery(Offer.class);
        Root<Offer> root = criteriaQuery.from(Offer.class);

        List<Predicate> predicates = buildPredicates(criteriaBuilder, root, criteria);

        criteriaQuery.where(predicates.toArray(new Predicate[0]));
        criteriaQuery.orderBy(buildOrder(criteriaBuilder, root, criteria.sortBy(), criteria.asc()));

        TypedQuery<Offer> query = entityManager.createQuery(criteriaQuery);
        query.setFirstResult(criteria.page() * criteria.size());
        query.setMaxResults(criteria.size());

        return query.getResultList();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder criteriaBuilder, Root<Offer> root, OfferSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        Optional.ofNullable(criteria.customerFirstName())
                .filter(name -> !name.isEmpty())
                .ifPresent(name -> predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("customerFirstName")), "%" + name.toLowerCase() + "%")));

        Optional.ofNullable(criteria.customerLastName())
                .filter(name -> !name.isEmpty())
                .ifPresent(name -> predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("customerLastName")), "%" + name.toLowerCase() + "%")));

        if (criteria.minPrice() != null || criteria.maxPrice() != null) {
            Double effectiveMinPrice = Optional.ofNullable(criteria.minPrice()).orElse(0.0);
            Double effectiveMaxPrice = Optional.ofNullable(criteria.maxPrice()).orElse(Double.MAX_VALUE);
            predicates.add(criteriaBuilder.between(root.get("price"), effectiveMinPrice, effectiveMaxPrice));
        }

        if (criteria.startDate() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("offerDate"), criteria.startDate()));
        }

        if (criteria.endDate() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("offerDate"), criteria.endDate()));
        }

        return predicates;
    }

    private Order buildOrder(CriteriaBuilder criteriaBuilder, Root<Offer> root, String sortBy, boolean asc) {
        String effectiveSortBy = Optional.ofNullable(sortBy).filter(s -> !s.isBlank()).orElse("id");
        return asc ? criteriaBuilder.asc(root.get(effectiveSortBy)) : criteriaBuilder.desc(root.get(effectiveSortBy));
    }


}