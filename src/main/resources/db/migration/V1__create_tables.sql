
CREATE TABLE IF NOT EXISTS cars (
                                    id BIGSERIAL PRIMARY KEY,
                                    brand VARCHAR(255) NOT NULL,
    year INTEGER CHECK (year >= 1886) NOT NULL,
    model VARCHAR(255) NOT NULL,
    color VARCHAR(255) NOT NULL,
    version BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fuel_type VARCHAR(50) NOT NULL,
    vin VARCHAR(255) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS offers (
                                      id BIGSERIAL PRIMARY KEY,
                                      customer_first_name VARCHAR(255) NOT NULL,
    customer_last_name VARCHAR(255) NOT NULL,
    price NUMERIC(19,2) NOT NULL CHECK (price > 0),
    offer_date TIMESTAMP NOT NULL,
    car_id BIGINT NOT NULL,
    version BIGINT,
    last_modified_offer TIMESTAMP,
    CONSTRAINT fk_car FOREIGN KEY (car_id) REFERENCES cars(id)
    );

CREATE TABLE IF NOT EXISTS REVINFO (
                                       REV BIGINT PRIMARY KEY,
                                       REVTSTMP BIGINT
);

CREATE SEQUENCE IF NOT EXISTS car_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS offer_seq  START 1;
DROP SEQUENCE IF EXISTS revinfo_seq;
CREATE SEQUENCE revinfo_seq INCREMENT BY 50 START 50;



ALTER TABLE cars ALTER COLUMN id SET DEFAULT nextval('car_id_seq');
ALTER TABLE offers ALTER COLUMN id SET DEFAULT nextval('offer_seq');