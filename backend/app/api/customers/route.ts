import type { NextRequest } from 'next/server';
import { json, preflight, requireDatabase, requirePassword } from '@/lib/http';
import { logRequest } from '@/lib/requestLog';
import { parseCustomer } from '@/lib/types';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

/** GET /api/customers — every customer, newest first. */
export async function GET(request: NextRequest) {
  await logRequest(request, 'GET /api/customers');
  const denied = requirePassword(request);
  if (denied) return denied;

  const db = await requireDatabase();
  if ('response' in db) return db.response;

  try {
    const customers = await db.store.list();
    return json({ customers, count: customers.length });
  } catch (error) {
    console.error('[GET /api/customers]', error);
    return json({ error: 'database_error', message: 'خوێندنەوە سەرکەوتوو نەبوو' }, 500);
  }
}

/**
 * POST /api/customers — idempotent upsert (INSERT ... ON CONFLICT (id) DO UPDATE).
 * Repeating the same request never creates duplicates, so retries are safe.
 */
export async function POST(request: NextRequest) {
  await logRequest(request, 'POST /api/customers');
  const denied = requirePassword(request);
  if (denied) return denied;

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return json({ error: 'invalid_json' }, 400);
  }

  const parsed = parseCustomer(body);
  if (!parsed.ok) {
    return json({ error: 'validation_failed', fields: parsed.errors }, 422);
  }

  const db = await requireDatabase();
  if ('response' in db) return db.response;

  try {
    const customer = await db.store.upsert(parsed.value);
    return json({ customer }, 200);
  } catch (error) {
    console.error('[POST /api/customers]', error);
    return json({ error: 'database_error', message: 'پاشەکەوتکردن سەرکەوتوو نەبوو' }, 500);
  }
}

export async function OPTIONS() {
  return preflight();
}
