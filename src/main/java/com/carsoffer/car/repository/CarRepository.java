package com.carsoffer.car.repository;

import com.carsoffer.car.dto.FuelType;
import com.carsoffer.car.entity.Car;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.util.*;

@ApplicationScoped
public class CarRepository implements PanacheRepository<Car> {

    private final EntityManager em;

    @Inject
    public CarRepository(EntityManager em) {
        this.em = em;
    }


    public PanacheQuery<Car> findByBrandAndModelPaged(String brand, String model, int page, int size) {
        String query = "select bm from Car bm where 1=1";
        Map<String, Object> params = new HashMap<>();

        if (brand != null && !brand.isBlank()) {
            query += " and LOWER(bm.brand) like LOWER(:brand)";
            params.put("brand", "%" + brand.toLowerCase() + "%");
        }
        if (model != null && !model.isBlank()) {
            query += " and LOWER(bm.model) like LOWER(:model)";
            params.put("model", "%" + model.toLowerCase() + "%");
        }

        return find(query, params).page(Page.of(page, size));
    }

    public PanacheQuery<Car> findByYearBetweenPaged(int startYear, int endYear, int page, int size) {
        return find("year >= :startYear and year <= :endYear",
                Parameters.with("startYear", startYear).and("endYear", endYear))
                .page(Page.of(page, size));

    }

    public PanacheQuery<Car> findAllPaged(int page, int size) {
        return findAll().page(Page.of(page, size));
    }


    public boolean existsByVin(String vin) {
        return find("vin", vin).firstResultOptional().isPresent();
    }


    public List<Car> searchCar(String brand, String model, Integer year, String color, FuelType fuelType, String sortBy, boolean asc, int page, int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Car> cq = cb.createQuery(Car.class);
        Root<Car> car = cq.from(Car.class);
        List<Predicate> predicates = new ArrayList<>();

        if (brand != null && !brand.isEmpty()) {
            predicates.add(cb.like(cb.lower(car.get("brand")), "%" + brand.toLowerCase() + "%"));
        }
        if (model != null && !model.isEmpty()) {
            predicates.add(cb.like(cb.lower(car.get("model")), "%" + model.toLowerCase() + "%"));
        }
        if (year != null) {
            predicates.add(cb.greaterThanOrEqualTo(car.get("year"), year));
        }
        if (color != null && !color.isEmpty()) {
            predicates.add(cb.like(cb.lower(car.get("color")), "%" + color.toLowerCase() + "%"));
        }
        if (fuelType != null) {
            predicates.add(cb.equal(car.get("fuelType"), fuelType));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "id";
        }
        Order order = asc ? cb.asc(car.get(sortBy)) : cb.desc(car.get(sortBy));
        cq.orderBy(order);

        TypedQuery<Car> query = em.createQuery(cq);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        return query.getResultList();
    }
}
