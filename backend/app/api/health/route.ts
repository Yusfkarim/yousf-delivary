import type { NextRequest } from 'next/server';
import { backends, describeError, getStore, isConfigured } from '@/lib/db';
import { json, preflight } from '@/lib/http';
import { passwordMatches } from '@/lib/password';
import type { ParsedCustomer } from '@/lib/types';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

/** Fixed UUID used by the write probe; it is removed again in the same request. */
const PROBE_ID = '00000000-0000-4000-8000-0000000000aa';

function probeCustomer(): ParsedCustomer {
  const now = Date.now();
  return {
    id: PROBE_ID,
    name: 'selftest probe',
    primaryPhone: '0000',
    hasWhatsapp: false,
    hasViber: false,
    extraPhones: [],
    latitude: 35.55,
    longitude: 45.43,
    addressNote: 'health check',
    createdAt: now,
    updatedAt: now,
  };
}

/**
 * Public liveness probe — exposes no customer data, only configuration state.
 *
 * `?selftest=1&password=<app password>` (or the `x-app-password` header) makes a
 * real round trip: it resolves the backend, creates the table when needed and
 * counts the rows. Adding `&write=1` also upserts and deletes a probe record, so
 * the exact code path the app uses for saving is exercised.
 */
export async function GET(request: NextRequest) {
  const base = {
    ok: true,
    service: 'yousif-delivery-api',
    databaseConfigured: isConfigured(),
    backends: backends(),
    serverTime: Date.now(),
  };

  const params = request.nextUrl.searchParams;
  if (params.get('selftest') !== '1') return json(base);

  const authorized =
    passwordMatches(request.headers.get('x-app-password')) ||
    passwordMatches(params.get('password'));
  if (!authorized) {
    return json({ ...base, error: 'unauthorized', message: 'وشەی نهێنی هەڵەیە' }, 401);
  }

  try {
    const store = await getStore();
    const customers = await store.list();
    const report: Record<string, unknown> = {
      ...base,
      databaseConfigured: true,
      dbReachable: true,
      backend: store.backend,
      host: store.detail,
      customers: customers.length,
    };

    if (params.get('write') === '1') {
      const saved = await store.upsert(probeCustomer());
      const removed = await store.remove(PROBE_ID);
      report.writeOk = saved.id === PROBE_ID && removed;
    }

    return json(report);
  } catch (error) {
    console.error('[GET /api/health?selftest=1]', error);
    return json(
      {
        ...base,
        dbReachable: false,
        error: 'database_error',
        detail: describeError(error),
      },
      503,
    );
  }
}

export async function OPTIONS() {
  return preflight();
}
