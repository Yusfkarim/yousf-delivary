import type { NextRequest } from 'next/server';
import { backends, describeError, getStore, isConfigured, lastProbeReport } from '@/lib/db';
import { json, preflight } from '@/lib/http';
import { appPassword, passwordMatches } from '@/lib/password';
import type { ParsedCustomer } from '@/lib/types';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

/** Fixed UUID used by the write probe; it is removed again in the same request. */
const PROBE_ID = '00000000-0000-4000-8000-0000000000aa';

function probeCustomer(): ParsedCustomer {
  const now = Date.now();
  return {
    id: PROBE_ID,
    kind: 'customer',
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
/**
 * End-to-end check of the routes the app actually calls.
 *
 * Vercel routes each API file to its own lambda, so a deployment can answer
 * `/api/health` and still miss `/api/customers` (an old build). Calling them
 * from inside the probe — authenticating with the same password — turns a
 * remote mystery into a plain status code.
 */
async function checkRoutes(origin: string, password: string) {
  const checks: Record<string, string> = {};
  const calls: Array<{ path: string; method: string; body?: string }> = [
    { path: '/api/customers', method: 'GET' },
    { path: '/api/auth/verify', method: 'POST', body: '{}' },
  ];
  for (const call of calls) {
    try {
      const response = await fetch(origin + call.path, {
        method: call.method,
        headers: {
          'x-app-password': password,
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: call.body,
        cache: 'no-store',
      });
      const text = await response.text();
      checks[call.path] = `${response.status} ${text.length}b`;
    } catch (error) {
      checks[call.path] = `error: ${error instanceof Error ? error.message : 'unknown'}`;
    }
  }
  return checks;
}

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

  const startedAt = Date.now();
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
      resolveMs: Date.now() - startedAt,
      probes: lastProbeReport(),
    };

    if (params.get('routes') === '1') {
      const origin = process.env.VERCEL_URL
        ? `https://${process.env.VERCEL_URL}`
        : request.nextUrl.origin;
      report.routeOrigin = origin;
      report.routes = await checkRoutes(origin, appPassword());
    }

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
        probes: lastProbeReport(),
      },
      503,
    );
  }
}

export async function OPTIONS() {
  return preflight();
}
