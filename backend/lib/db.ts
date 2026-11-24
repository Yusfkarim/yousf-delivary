import { createPool } from '@vercel/postgres';
import type { CustomerPayload, CustomerRow, ParsedCustomer } from './types';
import { rowToPayload } from './types';

type PgPool = ReturnType<typeof createPool>;

let pool: PgPool | null = null;
let schemaReady: Promise<void> | null = null;

/** Connection string, in Vercel's order of preference. */
export function connectionString(): string {
  return (
    process.env.POSTGRES_URL ||
    process.env.DATABASE_URL ||
    process.env.POSTGRES_URL_NON_POOLING ||
    process.env.POSTGRES_PRISMA_URL ||
    ''
  );
}

export function isConfigured(): boolean {
  return connectionString().length > 0;
}

export function getPool(): PgPool | null {
  if (!isConfigured()) return null;
  if (!pool) {
    pool = createPool({
      connectionString: connectionString(),
      max: 3,
      idleTimeoutMillis: 10_000,
      connectionTimeoutMillis: 10_000,
    });
  }
  return pool;
}

async function withRetry<T>(task: () => Promise<T>, attempts = 3): Promise<T> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      return await task();
    } catch (error) {
      lastError = error;
      if (attempt < attempts) await new Promise((resolve) => setTimeout(resolve, attempt * 350));
    }
  }
  throw lastError;
}

/** Creates the table on the first request; safe to call on every request. */
export async function ensureSchema(pg: PgPool): Promise<void> {
  if (!schemaReady) {
    schemaReady = (async () => {
      await pg.query(`
        CREATE TABLE IF NOT EXISTS customers (
          id             TEXT PRIMARY KEY,
          name           TEXT NOT NULL,
          primary_phone  TEXT NOT NULL,
          has_whatsapp   BOOLEAN NOT NULL DEFAULT FALSE,
          has_viber      BOOLEAN NOT NULL DEFAULT FALSE,
          extra_phones   JSONB NOT NULL DEFAULT '[]'::jsonb,
          latitude       DOUBLE PRECISION NOT NULL,
          longitude      DOUBLE PRECISION NOT NULL,
          address_note   TEXT NOT NULL DEFAULT '',
          created_at     BIGINT NOT NULL,
          updated_at     BIGINT NOT NULL
        )
      `);
      await pg.query(
        `CREATE INDEX IF NOT EXISTS customers_updated_at_idx ON customers (updated_at DESC)`,
      );
    })().catch((error) => {
      schemaReady = null;
      throw error;
    });
  }
  return schemaReady;
}

export async function listCustomers(pg: PgPool): Promise<CustomerPayload[]> {
  const result = await withRetry(() =>
    pg.query<CustomerRow>(
      `SELECT id, name, primary_phone, has_whatsapp, has_viber, extra_phones,
              latitude, longitude, address_note, created_at, updated_at
       FROM customers
       ORDER BY updated_at DESC`,
    ),
  );
  return result.rows.map(rowToPayload);
}

export async function upsertCustomer(pg: PgPool, customer: ParsedCustomer): Promise<CustomerPayload> {
  const result = await withRetry(() =>
    pg.query<CustomerRow>(
      `INSERT INTO customers
         (id, name, primary_phone, has_whatsapp, has_viber, extra_phones,
          latitude, longitude, address_note, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6::jsonb, $7, $8, $9, $10, $11)
       ON CONFLICT (id) DO UPDATE SET
         name = EXCLUDED.name,
         primary_phone = EXCLUDED.primary_phone,
         has_whatsapp = EXCLUDED.has_whatsapp,
         has_viber = EXCLUDED.has_viber,
         extra_phones = EXCLUDED.extra_phones,
         latitude = EXCLUDED.latitude,
         longitude = EXCLUDED.longitude,
         address_note = EXCLUDED.address_note,
         updated_at = EXCLUDED.updated_at
       RETURNING id, name, primary_phone, has_whatsapp, has_viber, extra_phones,
                 latitude, longitude, address_note, created_at, updated_at`,
      [
        customer.id,
        customer.name,
        customer.primaryPhone,
        customer.hasWhatsapp,
        customer.hasViber,
        JSON.stringify(customer.extraPhones),
        customer.latitude,
        customer.longitude,
        customer.addressNote,
        customer.createdAt,
        customer.updatedAt,
      ],
    ),
  );
  return rowToPayload(result.rows[0]);
}

export async function updateCustomer(
  pg: PgPool,
  id: string,
  customer: ParsedCustomer,
): Promise<CustomerPayload | null> {
  const result = await withRetry(() =>
    pg.query<CustomerRow>(
      `UPDATE customers SET
         name = $2,
         primary_phone = $3,
         has_whatsapp = $4,
         has_viber = $5,
         extra_phones = $6::jsonb,
         latitude = $7,
         longitude = $8,
         address_note = $9,
         updated_at = $10
       WHERE id = $1
       RETURNING id, name, primary_phone, has_whatsapp, has_viber, extra_phones,
                 latitude, longitude, address_note, created_at, updated_at`,
      [
        id,
        customer.name,
        customer.primaryPhone,
        customer.hasWhatsapp,
        customer.hasViber,
        JSON.stringify(customer.extraPhones),
        customer.latitude,
        customer.longitude,
        customer.addressNote,
        customer.updatedAt,
      ],
    ),
  );
  return result.rows.length > 0 ? rowToPayload(result.rows[0]) : null;
}

export async function deleteCustomer(pg: PgPool, id: string): Promise<boolean> {
  const result = await withRetry(() => pg.query(`DELETE FROM customers WHERE id = $1`, [id]));
  return (result.rowCount ?? 0) > 0;
}
