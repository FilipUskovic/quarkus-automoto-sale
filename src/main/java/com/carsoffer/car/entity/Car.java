package com.carsoffer.car.entity;

import com.carsoffer.car.dto.FuelType;
import com.carsoffer.offer.entity.Offer;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "cars", indexes = {
        @Index(name = "idx_car_brand_model", columnList = "brand, model"),
        @Index(name = "idx_year", columnList = "year")
})
@Audited
public class Car extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "carsSequence")
    @SequenceGenerator(name = "carsSequence", sequenceName = "car_id_seq", allocationSize = 1)
    @Column(name = "id", updatable = false, nullable = false)
    public Long id;


    @NotBlank
    @Column(name = "brand", nullable = false)
    private String brand;

    @Min(value = 1886, message = "Year cannot be older of 1886")
    @Column(name = "year", nullable = false)
    private Integer year;

    @NotBlank
    @Column(name = "model", nullable = false)
    private String model;

    @NotBlank
    @Column(name = "color", nullable = false)
    private String color;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Offer> offers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @NotBlank
    @NaturalId
    @Column(name = "vin", unique = true, nullable = false)
    private String vin;


    public void addOffer(Offer offer) {
        offers.add(offer);
        offer.setCar(this);

    }

    public void removeOffer(Offer offer) {
        offers.remove(offer);
        offer.setCar(null);
    }



    public Car() {}

    private Car(Builder builder) {
        this.id = builder.id;
        this.brand = builder.brand;
        this.model = builder.model;
        this.year = builder.year;
        this.color = builder.color;
        this.offers = builder.offers != null ? builder.offers : new HashSet<>();
        this.fuelType = builder.fuelType;
        this.vin = builder.vin;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank String getBrand() {
        return brand;
    }

    public void setBrand(@NotBlank String brand) {
        this.brand = brand;
    }

    public @Min(value = 1886, message = "Year cannot be older of 1886") Integer getYear() {
        return year;
    }

    public void setYear(@Min(value = 1886, message = "Year cannot be older of 1886") Integer year) {
        this.year = year;
    }

    public @NotBlank String getModel() {
        return model;
    }

    public void setModel(@NotBlank String model) {
        this.model = model;
    }

    public @NotBlank String getColor() {
        return color;
    }

    public void setColor(@NotBlank String color) {
        this.color = color;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Offer> getOffers() {
        return offers;
    }

    public void setOffers(Set<Offer> offers) {
        this.offers = offers;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return Objects.equals(id, car.id) && Objects.equals(brand, car.brand) && Objects.equals(model, car.model) && Objects.equals(vin, car.vin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, brand, model, vin);
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", brand='" + brand + '\'' +
                ", year=" + year +
                ", model='" + model + '\'' +
                ", color='" + color + '\'' +
                ", version=" + version +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", offers=" + offers +
                ", fuelType=" + fuelType +
                ", vin='" + vin + '\'' +
                '}';
    }

    public static class Builder {
        private Long id;
        private String brand;
        private String model;
        private Integer year;
        private String color;
        private Set<Offer> offers;
        private FuelType fuelType;
        private String vin;

       public Builder() {}

        public Builder id(Long id) {
           this.id = id;
           return this;
        }

        public Builder brand(String brand) {
           this.brand = brand;
           return this;
        }

        public Builder model(String model) {
           this.model = model;
           return this;
        }

        public Builder year(Integer year) {
           this.year = year;
           return this;
        }

        public Builder color(String color) {
           this.color = color;
           return this;
        }

        public Builder offers(Set<Offer> offers) {
           this.offers = offers;
           return this;
        }
        public Builder fuelType(FuelType fuelType) {
           this.fuelType = fuelType;
           return this;
        }

        public Builder vin(String vin){
           this.vin = vin;
           return this;
        }

        public Car build() {
           return new Car(this);
        }
    }
}
