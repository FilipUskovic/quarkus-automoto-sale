CREATE TABLE IF NOT EXISTS cars_aud (
   id BIGINT NOT NULL,
    brand VARCHAR(255),
    color VARCHAR(255),
    created_at TIMESTAMP,
    fuel_type VARCHAR(50),
    model VARCHAR(255),
    rev BIGINT NOT NULL,
    revtype SMALLINT,
    updated_at TIMESTAMP,
    vin VARCHAR(255),
    year INTEGER,
    PRIMARY KEY (id, rev)
    );

CREATE TABLE IF NOT EXISTS offers_aud (
    car_id BIGINT NOT NULL,
     customer_first_name VARCHAR(255),
    customer_last_name VARCHAR(255),
    last_modified_offer TIMESTAMP,
    offer_date TIMESTAMP,
    price DECIMAL(10, 2),
    revtype SMALLINT,
    id BIGINT NOT NULL,
    rev BIGINT NOT NULL,
    PRIMARY KEY (id, rev)
    );
