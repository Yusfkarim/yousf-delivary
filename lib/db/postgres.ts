import { createPool, type VercelPool } from '@vercel/postgres';
import { DELIVERY_SELECT, SCHEMA_SQL } from './schema';
import { splitSqlStatements } from './sql-utils';
import type { DatabaseHealth, DeliveryRepository } from './repository';
import type { Delivery, DeliveryInput, DeliveryPatch, DeliveryStatus } from '../types';

interface DeliveryRow {
  id: string;
  order_no: number | string | null;
  customer_id: string;
  name: string;
  primary_phone: string;
  extra_phones: unknown;
  latitude: number | string | null;
  longitude: number | string | null;
  address: string | null;
  landmark: string | null;
  channels: unknown;
  customer_notes: string | null;
  item_note: string | null;
  price: number | string | null;
  currency: string;
  delivery_fee: number | string | null;
  status: string;
  created_at: string | Date;
  updated_at: string | Date;
  delivered_at: string | Date | null;
}

type GlobalWithPool = typeof globalThis & {
  __ydPool?: VercelPool;
  __ydSchema?: Promise<void>;
};

const globalRef = globalThis as GlobalWithPool;

const IGNORABLE_SCHEMA_CODES = new Set([
  '42P07', // duplicate_table
  '42710', // duplicate_object
  '42P06', // duplicate_schema
  '42723', // duplicate_function
  '23505', // duplicate key value
]);

export function getConnectionString(): string | undefined {
  return (
    process.env.POSTGRES_URL ||
    process.env.DATABASE_URL ||
    process.env.POSTGRES_PRISMA_URL ||
    process.env.POSTGRES_URL_NON_POOLING ||
    undefined
  );
}

export function isPostgresConfigured(): boolean {
  return Boolean(getConnectionString());
}

function getPool(): VercelPool {
  const connectionString = getConnectionString();
  if (!connectionString) {
    throw new Error('POSTGRES_URL is not configured');
  }
  if (!globalRef.__ydPool) {
    globalRef.__ydPool = createPool({ connectionString, max: 4 });
  }
  return globalRef.__ydPool;
}

function isIgnorable(error: unknown): boolean {
  const candidate = error as { code?: string; message?: string } | null;
  if (!candidate) return false;
  if (candidate.code && IGNORABLE_SCHEMA_CODES.has(candidate.code)) return true;
  const message = candidate.message?.toLowerCase() ?? '';
  return message.includes('already exists');
}

/**
 * Creates every table, index and trigger on first use. Safe to call on each
 * request: it is memoised per process and every statement is idempotent.
 */
export async function ensureSchema(): Promise<void> {
  if (!globalRef.__ydSchema) {
    globalRef.__ydSchema = (async () => {
      const pool = getPool();
      const statements = splitSqlStatements(SCHEMA_SQL);
      for (const statement of statements) {
        try {
          await pool.query(statement);
        } catch (error) {
          if (!isIgnorable(error)) {
            globalRef.__ydSchema = undefined;
            throw error;
          }
        }
      }
    })();
  }
  return globalRef.__ydSchema;
}

function toNumberOrNull(value: number | string | null): number | null {
  if (value === null || value === '') return null;
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function toIso(value: string | Date | null): string | null {
  if (!value) return null;
  return value instanceof Date ? value.toISOString() : new Date(value).toISOString();
}

function parseJsonArray(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(String);
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value) as unknown;
      if (Array.isArray(parsed)) return parsed.map(String);
    } catch {
      return [];
    }
  }
  return [];
}

function parseChannels(value: unknown): Delivery['channels'] {
  const raw = Array.isArray(value) ? value.map(String) : parseJsonArray(value);
  const channels: Delivery['channels'] = [];
  if (raw.includes('whatsapp')) channels.push('whatsapp');
  if (raw.includes('viber')) channels.push('viber');
  return channels;
}

function mapRow(row: DeliveryRow): Delivery {
  const status = (['pending', 'on_the_way', 'delivered', 'canceled'] as const).includes(
    row.status as DeliveryStatus,
  )
    ? (row.status as DeliveryStatus)
    : 'pending';

  return {
    id: row.id,
    orderNo: toNumberOrNull(row.order_no) ?? 0,
    customerId: row.customer_id,
    name: row.name,
    primaryPhone: row.primary_phone,
    extraPhones: parseJsonArray(row.extra_phones),
    latitude: toNumberOrNull(row.latitude),
    longitude: toNumberOrNull(row.longitude),
    address: row.address ?? '',
    landmark: row.landmark ?? '',
    channels: parseChannels(row.channels),
    customerNotes: row.customer_notes ?? '',
    itemNote: row.item_note ?? '',
    price: toNumberOrNull(row.price) ?? 0,
    currency: row.currency === 'USD' ? 'USD' : 'IQD',
    deliveryFee: toNumberOrNull(row.delivery_fee) ?? 0,
    status,
    createdAt: toIso(row.created_at) ?? new Date().toISOString(),
    updatedAt: toIso(row.updated_at) ?? new Date().toISOString(),
    deliveredAt: toIso(row.delivered_at),
  };
}

const SELECT_ONE = `SELECT ${DELIVERY_SELECT}
  FROM orders o JOIN customers c ON c.id = o.customer_id
  WHERE o.id = $1 LIMIT 1`;

const SELECT_ALL = `SELECT ${DELIVERY_SELECT}
  FROM orders o JOIN customers c ON c.id = o.customer_id
  ORDER BY o.created_at DESC, o.order_no DESC
  LIMIT $1`;

async function fetchOne(id: string): Promise<Delivery | null> {
  const result = await getPool().query<DeliveryRow>(SELECT_ONE, [id]);
  const row = result.rows[0];
  return row ? mapRow(row) : null;
}

export const postgresRepository: DeliveryRepository = {
  kind: 'postgres',

  async list(limit = 1000) {
    await ensureSchema();
    const result = await getPool().query<DeliveryRow>(SELECT_ALL, [limit]);
    return result.rows.map(mapRow);
  },

  async get(id) {
    await ensureSchema();
    return fetchOne(id);
  },

  async create(input: DeliveryInput) {
    await ensureSchema();
    const pool = getPool();
    const client = await pool.connect();
    let orderId: string;

    try {
      await client.query('BEGIN');

      let customerId = input.customerId ?? null;
      if (customerId) {
        const existing = await client.query('SELECT id FROM customers WHERE id = $1', [customerId]);
        if (existing.rowCount === 0) customerId = null;
      }

      if (customerId) {
        await client.query(
          `UPDATE customers SET
             name = $1, primary_phone = $2, extra_phones = $3::jsonb,
             latitude = $4, longitude = $5, address = $6, landmark = $7,
             channels = $8::text[], notes = $9, updated_at = now()
           WHERE id = $10`,
          [
            input.name,
            input.primaryPhone,
            JSON.stringify(input.extraPhones),
            input.latitude,
            input.longitude,
            input.address,
            input.landmark,
            input.channels,
            input.customerNotes,
            customerId,
          ],
        );
      } else {
        const inserted = await client.query<{ id: string }>(
          `INSERT INTO customers
             (name, primary_phone, extra_phones, latitude, longitude, address, landmark, channels, notes)
           VALUES ($1, $2, $3::jsonb, $4, $5, $6, $7, $8::text[], $9)
           RETURNING id`,
          [
            input.name,
            input.primaryPhone,
            JSON.stringify(input.extraPhones),
            input.latitude,
            input.longitude,
            input.address,
            input.landmark,
            input.channels,
            input.customerNotes,
          ],
        );
        customerId = inserted.rows[0].id;
      }

      const order = await client.query<{ id: string }>(
        `INSERT INTO orders (customer_id, item_note, price, currency, delivery_fee, status, delivered_at)
         VALUES ($1, $2, $3, $4, $5, $6, CASE WHEN $6 = 'delivered' THEN now() ELSE NULL END)
         RETURNING id`,
        [
          customerId,
          input.itemNote,
          input.price,
          input.currency,
          input.deliveryFee,
          input.status,
        ],
      );

      orderId = order.rows[0].id;
      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK').catch(() => undefined);
      throw error;
    } finally {
      client.release();
    }

    const created = await fetchOne(orderId);
    if (!created) throw new Error('created order could not be read back');
    return created;
  },

  async update(id, patch: DeliveryPatch) {
    await ensureSchema();
    const current = await fetchOne(id);
    if (!current) return null;

    const merged: Delivery = {
      ...current,
      name: patch.name ?? current.name,
      primaryPhone: patch.primaryPhone ?? current.primaryPhone,
      extraPhones: patch.extraPhones ?? current.extraPhones,
      latitude: patch.latitude !== undefined ? patch.latitude : current.latitude,
      longitude: patch.longitude !== undefined ? patch.longitude : current.longitude,
      address: patch.address ?? current.address,
      landmark: patch.landmark ?? current.landmark,
      channels: patch.channels ?? current.channels,
      customerNotes: patch.customerNotes ?? current.customerNotes,
      itemNote: patch.itemNote ?? current.itemNote,
      price: patch.price ?? current.price,
      currency: patch.currency ?? current.currency,
      deliveryFee: patch.deliveryFee ?? current.deliveryFee,
      status: patch.status ?? current.status,
    };

    const pool = getPool();
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      await client.query(
        `UPDATE customers SET
           name = $1, primary_phone = $2, extra_phones = $3::jsonb,
           latitude = $4, longitude = $5, address = $6, landmark = $7,
           channels = $8::text[], notes = $9, updated_at = now()
         WHERE id = $10`,
        [
          merged.name,
          merged.primaryPhone,
          JSON.stringify(merged.extraPhones),
          merged.latitude,
          merged.longitude,
          merged.address,
          merged.landmark,
          merged.channels,
          merged.customerNotes,
          merged.customerId,
        ],
      );

      const statusChanged = patch.status !== undefined && patch.status !== current.status;
      await client.query(
        `UPDATE orders SET
           item_note = $1, price = $2, currency = $3, delivery_fee = $4,
           status = $5,
           delivered_at = CASE
             WHEN $5 = 'delivered' THEN COALESCE(delivered_at, now())
             ELSE NULL
           END,
           updated_at = now()
         WHERE id = $6`,
        [
          merged.itemNote,
          merged.price,
          merged.currency,
          merged.deliveryFee,
          merged.status,
          id,
        ],
      );
      void statusChanged;
      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK').catch(() => undefined);
      throw error;
    } finally {
      client.release();
    }

    return fetchOne(id);
  },

  async setStatus(id: string, status: DeliveryStatus) {
    await ensureSchema();
    const result = await getPool().query(
      `UPDATE orders SET
         status = $1,
         delivered_at = CASE WHEN $1 = 'delivered' THEN COALESCE(delivered_at, now()) ELSE NULL END,
         updated_at = now()
       WHERE id = $2`,
      [status, id],
    );
    if (result.rowCount === 0) return null;
    return fetchOne(id);
  },

  async remove(id) {
    await ensureSchema();
    const pool = getPool();
    const result = await pool.query<{ customer_id: string }>(
      'DELETE FROM orders WHERE id = $1 RETURNING customer_id',
      [id],
    );
    const customerId = result.rows[0]?.customer_id;
    if (customerId) {
      // remove the customer too when this was their last order
      const remaining = await pool.query<{ count: string }>(
        'SELECT COUNT(*)::text AS count FROM orders WHERE customer_id = $1',
        [customerId],
      );
      if (Number(remaining.rows[0]?.count ?? '0') === 0) {
        await pool.query('DELETE FROM customers WHERE id = $1', [customerId]);
      }
    }
    return (result.rowCount ?? 0) > 0;
  },

  async seedDemo() {
    await ensureSchema();
    const pool = getPool();
    const { DEMO_DELIVERIES } = await import('../demo');
    const now = Date.now();
    let inserted = 0;

    for (const demo of DEMO_DELIVERIES) {
      const createdAt = new Date(now - demo.minutesAgo * 60_000);
      const deliveredAt =
        demo.status === 'delivered' ? new Date(createdAt.getTime() + 18 * 60_000) : null;

      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        const customer = await client.query<{ id: string }>(
          `INSERT INTO customers
             (name, primary_phone, extra_phones, latitude, longitude, address, landmark, channels, notes, created_at)
           VALUES ($1, $2, $3::jsonb, $4, $5, $6, $7, $8::text[], $9, $10)
           RETURNING id`,
          [
            demo.name,
            demo.primaryPhone,
            JSON.stringify(demo.extraPhones),
            demo.latitude,
            demo.longitude,
            demo.address,
            demo.landmark,
            demo.channels,
            demo.customerNotes,
            createdAt,
          ],
        );

        await client.query(
          `INSERT INTO orders
             (customer_id, item_note, price, currency, delivery_fee, status, created_at, updated_at, delivered_at)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $7, $8)`,
          [
            customer.rows[0].id,
            demo.itemNote,
            demo.price,
            demo.currency,
            demo.deliveryFee,
            demo.status,
            createdAt,
            deliveredAt,
          ],
        );
        await client.query('COMMIT');
        inserted += 1;
      } catch (error) {
        await client.query('ROLLBACK').catch(() => undefined);
        throw error;
      } finally {
        client.release();
      }
    }

    return inserted;
  },

  async count() {
    await ensureSchema();
    const result = await getPool().query<{ count: string }>('SELECT COUNT(*)::text AS count FROM orders');
    return Number(result.rows[0]?.count ?? '0');
  },

  async health(): Promise<DatabaseHealth> {
    try {
      await ensureSchema();
      const result = await getPool().query<{ count: string }>('SELECT COUNT(*)::text AS count FROM orders');
      return {
        ok: true,
        backend: 'postgres',
        schemaReady: true,
        message: `${Number(result.rows[0]?.count ?? '0')} orders`,
      };
    } catch (error) {
      return {
        ok: false,
        backend: 'postgres',
        schemaReady: false,
        message: error instanceof Error ? error.message : 'unknown error',
      };
    }
  },
};
