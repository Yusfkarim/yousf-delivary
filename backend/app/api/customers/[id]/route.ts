import type { NextRequest } from 'next/server';
import { json, preflight, requireDatabase, requirePassword } from '@/lib/http';
import { isUuid, parseCustomer } from '@/lib/types';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

interface RouteContext {
  params: Promise<{ id: string }>;
}

/** PUT /api/customers/[id] — atomic update, returns the saved record. */
export async function PUT(request: NextRequest, context: RouteContext) {
  const denied = requirePassword(request);
  if (denied) return denied;

  const { id } = await context.params;
  if (!isUuid(id)) return json({ error: 'invalid_id' }, 400);

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return json({ error: 'invalid_json' }, 400);
  }

  const parsed = parseCustomer(body, { id });
  if (!parsed.ok) {
    return json({ error: 'validation_failed', fields: parsed.errors }, 422);
  }

  const db = await requireDatabase();
  if ('response' in db) return db.response;

  try {
    const customer = await db.store.update(id, parsed.value);
    if (!customer) return json({ error: 'not_found' }, 404);
    return json({ customer }, 200);
  } catch (error) {
    console.error('[PUT /api/customers/:id]', error);
    return json({ error: 'database_error', message: 'نوێکردنەوە سەرکەوتوو نەبوو' }, 500);
  }
}

/** DELETE /api/customers/[id] — permanent delete. */
export async function DELETE(request: NextRequest, context: RouteContext) {
  const denied = requirePassword(request);
  if (denied) return denied;

  const { id } = await context.params;
  if (!isUuid(id)) return json({ error: 'invalid_id' }, 400);

  const db = await requireDatabase();
  if ('response' in db) return db.response;

  try {
    const deleted = await db.store.remove(id);
    if (!deleted) return json({ error: 'not_found', id }, 404);
    return json({ ok: true, deleted: true, id }, 200);
  } catch (error) {
    console.error('[DELETE /api/customers/:id]', error);
    return json({ error: 'database_error', message: 'سڕینەوە سەرکەوتوو نەبوو' }, 500);
  }
}

export async function OPTIONS() {
  return preflight();
}
