-- ---------------------------------------------------------------------------
-- یوسف دلیڤەری (Yousif Delivery) — database schema
-- Generated from lib/db/schema.ts by `npm run schema:build`. Do not edit.
--
-- The application runs this automatically on the first request, so applying it
-- manually is optional. To do it by hand:
--   psql "$POSTGRES_URL" -f schema.sql
-- ---------------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS customers (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name          TEXT NOT NULL,
  primary_phone TEXT NOT NULL,
  extra_phones  JSONB NOT NULL DEFAULT '[]'::jsonb,
  latitude      DOUBLE PRECISION,
  longitude     DOUBLE PRECISION,
  address       TEXT NOT NULL DEFAULT '',
  landmark      TEXT NOT NULL DEFAULT '',
  channels      TEXT[] NOT NULL DEFAULT ARRAY['whatsapp']::text[],
  notes         TEXT NOT NULL DEFAULT '',
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_no     SERIAL,
  customer_id  UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  item_note    TEXT NOT NULL DEFAULT '',
  price        NUMERIC(12, 2) NOT NULL DEFAULT 0,
  currency     TEXT NOT NULL DEFAULT 'IQD',
  delivery_fee NUMERIC(12, 2) NOT NULL DEFAULT 0,
  status       TEXT NOT NULL DEFAULT 'pending',
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  delivered_at TIMESTAMPTZ,
  CONSTRAINT orders_status_check CHECK (status IN ('pending', 'on_the_way', 'delivered', 'canceled')),
  CONSTRAINT orders_currency_check CHECK (currency IN ('IQD', 'USD'))
);

CREATE INDEX IF NOT EXISTS orders_status_idx ON orders (status);
CREATE INDEX IF NOT EXISTS orders_created_at_idx ON orders (created_at DESC);
CREATE INDEX IF NOT EXISTS orders_customer_id_idx ON orders (customer_id);
CREATE INDEX IF NOT EXISTS orders_delivered_at_idx ON orders (delivered_at);
CREATE INDEX IF NOT EXISTS customers_primary_phone_idx ON customers (primary_phone);

-- Keep updated_at fresh without application code.
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS customers_set_updated_at ON customers;
CREATE TRIGGER customers_set_updated_at
  BEFORE UPDATE ON customers
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS orders_set_updated_at ON orders;
CREATE TRIGGER orders_set_updated_at
  BEFORE UPDATE ON orders
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
