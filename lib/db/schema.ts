/**
 * Canonical database schema for یوسف دلیڤەری.
 *
 * This string is the single source of truth. `schema.sql` is generated from it
 * (`npm run schema:build`) and it is executed automatically on the first query
 * so a fresh Vercel Postgres database is ready with zero manual steps.
 */
export const SCHEMA_SQL = /* sql */ `
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
`;

/** Column list used by every SELECT so client and server stay in sync. */
export const DELIVERY_SELECT = /* sql */ `
  o.id                AS id,
  o.order_no          AS order_no,
  o.customer_id       AS customer_id,
  c.name              AS name,
  c.primary_phone     AS primary_phone,
  c.extra_phones      AS extra_phones,
  c.latitude          AS latitude,
  c.longitude         AS longitude,
  c.address           AS address,
  c.landmark          AS landmark,
  c.channels          AS channels,
  c.notes             AS customer_notes,
  o.item_note         AS item_note,
  o.price             AS price,
  o.currency          AS currency,
  o.delivery_fee      AS delivery_fee,
  o.status            AS status,
  o.created_at        AS created_at,
  o.updated_at        AS updated_at,
  o.delivered_at      AS delivered_at
`;
