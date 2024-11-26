package com.carsoffer.offer.entity;

import com.carsoffer.car.entity.Car;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "offers", indexes = {
        @Index(name = "idx_offer_price", columnList = "price"),
        @Index(name = "idx_offer_car_id", columnList = "car_id"),
        @Index(name = "idx_customer_name", columnList = "customer_first_name, customer_last_name")
})

@Audited
@NamedEntityGraph(name = "Offer.car",
        attributeNodes = @NamedAttributeNode("car"))
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "offerSeq")
    @SequenceGenerator(name = "offerSeq", sequenceName = "offer_seq", allocationSize = 1)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotBlank
    @Column(name = "customer_first_name", nullable = false)
    private String customerFirstName;

    @NotBlank
    @Column(name = "customer_last_name", nullable = false)
    private String customerLastName;

    @Positive
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @NotNull
    @Column(name = "offer_date", nullable = false)
    private LocalDateTime offerDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Version
    @Column(name = "version")
    private Long version;


    @Column(name = "last_modified_offer", nullable = true)
    private LocalDateTime lastModifiedOffer;


    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedOffer = LocalDateTime.now();
    }



    public Offer() {}

    private Offer(Builder builder) {
        this.id = builder.id;
        this.car = builder.car;
        this.customerFirstName = builder.customerFirstName;
        this.customerLastName = builder.customerLastName;
        this.price = builder.price;
        this.offerDate = builder.offerDate;
        this.lastModifiedOffer = builder.lastModifiedDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank String getCustomerFirstName() {
        return customerFirstName;
    }

    public void setCustomerFirstName(@NotBlank String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }

    public @NotBlank String getCustomerLastName() {
        return customerLastName;
    }

    public void setCustomerLastName(@NotBlank String customerLastName) {
        this.customerLastName = customerLastName;
    }

    public @Positive BigDecimal getPrice() {
        return price;
    }

    public void setPrice(@Positive BigDecimal price) {
        this.price = price;
    }

    public @NotNull LocalDateTime getOfferDate() {
        return offerDate;
    }

    public void setOfferDate(@NotNull LocalDateTime offerDate) {
        this.offerDate = offerDate;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }


    public LocalDateTime getLastModifiedOffer() {
        return lastModifiedOffer;
    }

    public void setLastModifiedOffer(LocalDateTime lastModifiedOffer) {
        this.lastModifiedOffer = lastModifiedOffer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Offer offer = (Offer) o;
        return Objects.equals(id, offer.id) && Objects.equals(customerFirstName, offer.customerFirstName) && Objects.equals(customerLastName, offer.customerLastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerFirstName, customerLastName);
    }

    @Override
    public String toString() {
        return "Offer{" +
                "id=" + id +
                ", customerFirstName='" + customerFirstName + '\'' +
                ", customerLastName='" + customerLastName + '\'' +
                ", price=" + price +
                ", offerDate=" + offerDate +
                ", car=" + car +
                ", version=" + version +
                ", lastModifiedDate=" + lastModifiedOffer +
                '}';
    }

    public static class Builder {
        private Long id;
        private String customerFirstName;
        private String customerLastName;
        private BigDecimal price;
        private LocalDateTime offerDate;
        private Car car;
        private LocalDateTime lastModifiedDate;

        public Builder() {}

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder customerFirstName(String customerFirstName) {
            this.customerFirstName = customerFirstName;
            return this;
        }

        public Builder customerLastName(String customerLastName) {
            this.customerLastName = customerLastName;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder offerDate(LocalDateTime offerDate) {
            this.offerDate = offerDate;
            return this;
        }

        public Builder car(Car car) {
            this.car = car;
            return this;
        }

        public Builder lastModifiedDate(LocalDateTime lastModifiedDate){
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        public Offer build() {
            return new Offer(this);
        }
    }
}
