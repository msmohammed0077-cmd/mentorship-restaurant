CREATE UNIQUE INDEX uq_addresses_one_default_per_customer
    ON addresses (customer_id)
    WHERE address_is_default;
