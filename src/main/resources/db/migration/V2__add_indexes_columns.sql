CREATE INDEX IF NOT EXISTS idx_car_brand_model ON cars (brand, model);
CREATE INDEX IF NOT EXISTS idx_year ON cars (year);

CREATE INDEX IF NOT EXISTS idx_offer_price ON offers (price);
CREATE INDEX IF NOT EXISTS idx_offer_car_id ON offers (car_id);
CREATE INDEX IF NOT EXISTS idx_customer_name ON offers (customer_first_name, customer_last_name);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'cars' AND column_name = 'vin'
    ) THEN
        ALTER TABLE cars ADD COLUMN vin VARCHAR(17);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'cars' AND column_name = 'fuel_type'
    ) THEN
        ALTER TABLE cars ADD COLUMN fuel_type VARCHAR(255);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'cars' AND column_name = 'version'
    ) THEN
        ALTER TABLE cars ADD COLUMN version BIGINT;
    END IF;
END $$;

UPDATE cars SET vin = vin || '_' || id WHERE vin = 'UNKNOWN';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE tablename = 'cars' AND indexname = 'vin_unique_idx'
    ) THEN
        CREATE UNIQUE INDEX vin_unique_idx ON cars(vin);
    END IF;
END $$;