/**
 * Storage layer with two interchangeable backends:
 *
 *   1. `postgres` — any Postgres connection string (Vercel Postgres, Neon,
 *      Supabase's direct/pooler URL, ...). Used when one of the POSTGRES_* /
 *      DATABASE_URL variables holds a value.
 *   2. `supabase` — Supabase's REST API (PostgREST) with the service-role key.
 *      No database password needed, so it still works when the Postgres URL
 *      was never injected into the project.
 *
 * The routes only know `CustomerStore`, so switching backends never touches
 * them. The first backend that answers a real query wins and is remembered for
 * the lifetime of the serverless instance.
 */
import { createPool } from '@vercel/postgres';
import type { CustomerPayload, CustomerRow, ParsedCustomer } from './types';
import { rowToPayload } from './types';

export type BackendName = 'postgres' | 'supabase' | null;

export interface CustomerStore {
  readonly backend: Exclude<BackendName, null>;
  /** Host or project the backend talks to — safe to show in diagnostics. */
  readonly detail: string;
  list(): Promise<CustomerPayload[]>;
  upsert(customer: ParsedCustomer): Promise<CustomerPayload>;
  update(id: string, customer: ParsedCustomer): Promise<CustomerPayload | null>;
  remove(id: string): Promise<boolean>;
  /** Throws when the backend is not reachable / the table is missing. */
  ping(): Promise<void>;
}

type PgPool = ReturnType<typeof createPool>;

const TABLE = 'customers';
const COLUMNS =
  'id,name,primary_phone,has_whatsapp,has_viber,extra_phones,latitude,longitude,address_note,created_at,updated_at';

function clean(value: string | undefined | null): string {
  return typeof value === 'string' ? value.trim() : '';
}

function hostOf(url: string): string {
  try {
    return new URL(url).hostname;
  } catch {
    return 'postgres';
  }
}

/** Managed providers always require TLS and their CA chain is not bundled. */
function needsRelaxedTls(url: string): boolean {
  const host = hostOf(url);
  return (
    host.endsWith('.supabase.co') ||
    host.endsWith('.supabase.com') ||
    host.includes('pooler.supabase') ||
    host.endsWith('.neon.tech') ||
    host.endsWith('.vercel-storage.com')
  );
}

/** Adds `sslmode=no-verify` when the URL does not already decide the mode. */
function normalizeUrl(raw: string): string {
  const url = clean(raw);
  if (!/^postgres(ql)?:\/\//i.test(url)) return '';
  if (/[?&]sslmode=/i.test(url)) return url;
  return `${url}${url.includes('?') ? '&' : '?'}sslmode=no-verify`;
}

/** Postgres connection strings in order of preference, deduplicated. */
export function postgresUrls(): string[] {
  const candidates = [
    process.env.POSTGRES_URL,
    process.env.POSTGRES_URL_NON_POOLING,
    process.env.DATABASE_URL,
    process.env.POSTGRES_PRISMA_URL,
  ];
  const urls: string[] = [];
  for (const candidate of candidates) {
    const url = normalizeUrl(candidate ?? '');
    if (url && !urls.includes(url)) urls.push(url);
  }
  return urls;
}

/** Supabase REST credentials, when present. */
export function supabaseConfig(): { url: string; key: string } | null {
  const url =
    clean(process.env.DELIVERY_SUPABASE_URL) ||
    clean(process.env.SUPABASE_URL) ||
    clean(process.env.NEXT_PUBLIC_SUPABASE_URL);
  const key =
    clean(process.env.DELIVERY_SUPABASE_SERVICE_KEY) ||
    clean(process.env.SUPABASE_SERVICE_ROLE_KEY) ||
    clean(process.env.SUPABASE_SECRET_KEY);
  if (!url || !key) return null;
  return { url: url.replace(/\/+$/, ''), key };
}

/** True when the Supabase credentials were set explicitly for this app. */
export function supabasePreferred(): boolean {
  const explicitUrl = clean(process.env.DELIVERY_SUPABASE_URL);
  const explicitKey =
    clean(process.env.DELIVERY_SUPABASE_SERVICE_KEY) || clean(process.env.SUPABASE_SECRET_KEY);
  return explicitUrl !== '' && explicitKey !== '';
}

/** True when at least one backend has credentials. Cheap, no network call. */
export function isConfigured(): boolean {
  return postgresUrls().length > 0 || supabaseConfig() !== null;
}

// ── Postgres ────────────────────────────────────────────────────────────────

/**
 * Retries are deliberately short: a serverless request must not spend 30s
 * waiting for a misconfigured backend before the fallback gets a chance.
 */
async function withRetry<T>(task: () => Promise<T>, attempts = 2): Promise<T> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      return await task();
    } catch (error) {
      lastError = error;
      if (attempt < attempts) await new Promise((resolve) => setTimeout(resolve, 150));
    }
  }
  throw lastError;
}

const CREATE_TABLE = `
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
  )`;

function createPostgresStore(url: string): CustomerStore {
  let pool: PgPool | null = null;
  let schemaReady: Promise<void> | null = null;

  function connect(): PgPool {
    if (!pool) {
      pool = createPool({
        connectionString: url,
        max: 3,
        idleTimeoutMillis: 10_000,
        connectionTimeoutMillis: 4_000,
        ssl: needsRelaxedTls(url) ? { rejectUnauthorized: false } : undefined,
      });
    }
    return pool;
  }

  async function ready(): Promise<PgPool> {
    const pg = connect();
    if (!schemaReady) {
      schemaReady = (async () => {
        await pg.query(CREATE_TABLE);
        await pg.query(
          `CREATE INDEX IF NOT EXISTS customers_updated_at_idx ON customers (updated_at DESC)`,
        );
      })().catch((error) => {
        schemaReady = null;
        throw error;
      });
    }
    await schemaReady;
    return pg;
  }

  return {
    backend: 'postgres',
    detail: hostOf(url),

    async ping() {
      const pg = connect();
      await withRetry(() => pg.query('SELECT 1'));
    },

    async list() {
      return withRetry(async () => {
        const pg = await ready();
        const result = await pg.query<CustomerRow>(
          `SELECT ${COLUMNS} FROM customers ORDER BY updated_at DESC`,
        );
        return result.rows.map(rowToPayload);
      });
    },

    async upsert(customer) {
      return withRetry(async () => {
        const pg = await ready();
        const result = await pg.query<CustomerRow>(
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
           RETURNING ${COLUMNS}`,
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
        );
        return rowToPayload(result.rows[0]);
      });
    },

    async update(id, customer) {
      return withRetry(async () => {
        const pg = await ready();
        const result = await pg.query<CustomerRow>(
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
           RETURNING ${COLUMNS}`,
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
        );
        return result.rows.length > 0 ? rowToPayload(result.rows[0]) : null;
      });
    },

    async remove(id) {
      return withRetry(async () => {
        const pg = await ready();
        const result = await pg.query(`DELETE FROM customers WHERE id = $1`, [id]);
        return (result.rowCount ?? 0) > 0;
      });
    },
  };
}

// ── Supabase REST (PostgREST) ───────────────────────────────────────────────

function toRow(customer: ParsedCustomer): CustomerRow {
  return {
    id: customer.id,
    name: customer.name,
    primary_phone: customer.primaryPhone,
    has_whatsapp: customer.hasWhatsapp,
    has_viber: customer.hasViber,
    extra_phones: customer.extraPhones,
    latitude: customer.latitude,
    longitude: customer.longitude,
    address_note: customer.addressNote,
    created_at: customer.createdAt,
    updated_at: customer.updatedAt,
  };
}

function createSupabaseStore(config: { url: string; key: string }): CustomerStore {
  async function call(path: string, init: RequestInit = {}): Promise<unknown> {
    const response = await fetch(`${config.url}/rest/v1/${TABLE}${path}`, {
      ...init,
      headers: {
        apikey: config.key,
        Authorization: `Bearer ${config.key}`,
        'Content-Type': 'application/json',
        ...((init.headers as Record<string, string> | undefined) ?? {}),
      },
      cache: 'no-store',
    });
    const text = await response.text();
    if (!response.ok) {
      throw new Error(`supabase_rest_${response.status}: ${text.slice(0, 240)}`);
    }
    if (!text) return null;
    try {
      return JSON.parse(text);
    } catch {
      return null;
    }
  }

  function rows(value: unknown): CustomerRow[] {
    return Array.isArray(value) ? (value as CustomerRow[]) : [];
  }

  return {
    backend: 'supabase',
    detail: `${hostOf(config.url)} (REST)`,

    async ping() {
      await call(`?select=id&limit=1`);
    },

    async list() {
      const data = await call(`?select=${COLUMNS}&order=updated_at.desc`);
      return rows(data).map(rowToPayload);
    },

    async upsert(customer) {
      const data = await call(`?on_conflict=id&select=${COLUMNS}`, {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify(toRow(customer)),
      });
      const first = rows(data)[0];
      if (!first) throw new Error('supabase_rest_empty_response');
      return rowToPayload(first);
    },

    async update(id, customer) {
      const data = await call(`?id=eq.${encodeURIComponent(id)}&select=${COLUMNS}`, {
        method: 'PATCH',
        headers: { Prefer: 'return=representation' },
        body: JSON.stringify(toRow({ ...customer, id })),
      });
      const first = rows(data)[0];
      return first ? rowToPayload(first) : null;
    },

    async remove(id) {
      const data = await call(`?id=eq.${encodeURIComponent(id)}&select=id`, {
        method: 'DELETE',
        headers: { Prefer: 'return=representation' },
      });
      return rows(data).length > 0;
    },
  };
}

// ── Resolution ──────────────────────────────────────────────────────────────

export function describeError(error: unknown): string {
  const message = error instanceof Error ? error.message : String(error);
  return message.replace(/postgres(ql)?:\/\/\S+/gi, '[url]').slice(0, 400);
}

/** One probe attempt, kept for /api/health?selftest=1 diagnostics. */
export interface ProbeResult {
  backend: 'postgres' | 'supabase';
  host: string;
  ok: boolean;
  ms: number;
  error?: string;
}

let lastProbes: ProbeResult[] = [];

export function lastProbeReport(): ProbeResult[] {
  return lastProbes;
}

/** Connection strings that already failed in this instance — never retried. */
const failedUrls = new Set<string>();

interface Candidate {
  backend: 'postgres' | 'supabase';
  url?: string;
  make: () => CustomerStore;
}

async function resolveStore(): Promise<CustomerStore> {
  const candidates: Candidate[] = [];

  const postgresCandidates: Candidate[] = postgresUrls()
    .filter((url) => !failedUrls.has(url))
    .map((url) => ({ backend: 'postgres', url, make: () => createPostgresStore(url) }));

  const config = supabaseConfig();
  const supabaseCandidate: Candidate[] = config
    ? [{ backend: 'supabase', make: () => createSupabaseStore(config) }]
    : [];

  // An explicitly configured Supabase project wins: it is the backend we know
  // works, so a stale POSTGRES_URL can never delay the first request.
  const ordered = supabasePreferred()
    ? [...supabaseCandidate, ...postgresCandidates]
    : [...postgresCandidates, ...supabaseCandidate];

  const problems: string[] = [];
  const probes: ProbeResult[] = [];

  for (const candidate of ordered) {
    const store = candidate.make();
    const started = Date.now();
    try {
      await store.ping();
      probes.push({ backend: store.backend, host: store.detail, ok: true, ms: Date.now() - started });
      lastProbes = probes;
      return store;
    } catch (error) {
      const message = describeError(error);
      probes.push({
        backend: store.backend,
        host: store.detail,
        ok: false,
        ms: Date.now() - started,
        error: message,
      });
      if (candidate.backend === 'postgres' && candidate.url) failedUrls.add(candidate.url);
      problems.push(`${store.backend} ${store.detail}: ${message}`);
    }
  }

  lastProbes = probes;
  throw new Error(problems.length > 0 ? problems.join(' | ') : 'no database configured');
}

let resolved: Promise<CustomerStore> | null = null;

/** The active store, resolved once per serverless instance. */
export function getStore(): Promise<CustomerStore> {
  if (!resolved) {
    resolved = resolveStore().catch((error) => {
      resolved = null;
      throw error;
    });
  }
  return resolved;
}

/** Which backends have credentials — sync, for /api/health. */
export function backends(): { postgres: boolean; supabase: boolean } {
  return { postgres: postgresUrls().length > 0, supabase: supabaseConfig() !== null };
}
