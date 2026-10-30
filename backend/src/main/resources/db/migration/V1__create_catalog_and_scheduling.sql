CREATE TABLE category (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(80)  NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE product (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(140) NOT NULL UNIQUE,
    description TEXT         NOT NULL DEFAULT '',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE product_category (
    product_id  BIGINT NOT NULL REFERENCES product (id),
    category_id BIGINT NOT NULL REFERENCES category (id),
    PRIMARY KEY (product_id, category_id)
);

CREATE TABLE product_variant (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT         NOT NULL REFERENCES product (id),
    name       VARCHAR(60)    NOT NULL,
    price      NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    active     BOOLEAN        NOT NULL DEFAULT TRUE,
    sort_order INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT uq_variant_product_name UNIQUE (product_id, name)
);

CREATE TABLE delivery_zone (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(80)    NOT NULL UNIQUE,
    shipping_fee NUMERIC(12, 2) NOT NULL CHECK (shipping_fee >= 0),
    active       BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE delivery_slot (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slot_date  DATE    NOT NULL,
    start_time TIME    NOT NULL,
    end_time   TIME    NOT NULL,
    capacity   INTEGER NOT NULL CHECK (capacity >= 0),
    booked     INTEGER NOT NULL DEFAULT 0 CHECK (booked >= 0),
    is_open    BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_slot_booked_le_capacity CHECK (booked <= capacity),
    CONSTRAINT ck_slot_time_order CHECK (end_time > start_time),
    CONSTRAINT uq_slot_date_start UNIQUE (slot_date, start_time)
);

CREATE TABLE shop_settings (
    id                    SMALLINT    PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    max_units_per_line    INTEGER     NOT NULL DEFAULT 10 CHECK (max_units_per_line > 0),
    max_units_per_order   INTEGER     NOT NULL DEFAULT 20 CHECK (max_units_per_order > 0),
    default_slot_capacity INTEGER     NOT NULL DEFAULT 4  CHECK (default_slot_capacity >= 0),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO shop_settings DEFAULT VALUES;