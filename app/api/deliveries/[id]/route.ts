import { db } from '@/lib/db';
import { isValidId, jsonError, jsonOk, readJsonBody } from '@/lib/http';
import { t } from '@/lib/i18n';
import { validateDeliveryPatch } from '@/lib/validation';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

type RouteContext = { params: Promise<{ id: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { id } = await context.params;
  if (!isValidId(id)) return jsonError(t.notFound, 400, { code: 'invalid_id' });

  try {
    const delivery = await db.get(id);
    if (!delivery) return jsonError(t.notFound, 404, { code: 'not_found' });
    return jsonOk({ delivery });
  } catch (error) {
    console.error('[GET /api/deliveries/:id]', error);
    return jsonError(t.errorRetry, 500, { code: 'read_failed' });
  }
}

export async function PATCH(request: Request, context: RouteContext) {
  const { id } = await context.params;
  if (!isValidId(id)) return jsonError(t.notFound, 400, { code: 'invalid_id' });

  const body = await readJsonBody(request);
  const validation = validateDeliveryPatch(body);

  if (!validation.ok || !validation.data) {
    return jsonError(t.errorRetry, 422, {
      fields: validation.fields ?? {},
      code: 'validation_failed',
    });
  }

  const patch = validation.data;
  const keys = Object.keys(patch);

  try {
    let delivery;
    if (keys.length === 1 && typeof patch.status === 'string') {
      delivery = await db.setStatus(id, patch.status as never);
    } else {
      delivery = await db.update(id, patch as never);
    }

    if (!delivery) return jsonError(t.notFound, 404, { code: 'not_found' });
    return jsonOk({ delivery });
  } catch (error) {
    console.error('[PATCH /api/deliveries/:id]', error);
    return jsonError(t.errorRetry, 500, { code: 'update_failed' });
  }
}

export async function DELETE(_request: Request, context: RouteContext) {
  const { id } = await context.params;
  if (!isValidId(id)) return jsonError(t.notFound, 400, { code: 'invalid_id' });

  try {
    const removed = await db.remove(id);
    if (!removed) return jsonError(t.notFound, 404, { code: 'not_found' });
    return jsonOk({ ok: true });
  } catch (error) {
    console.error('[DELETE /api/deliveries/:id]', error);
    return jsonError(t.errorRetry, 500, { code: 'delete_failed' });
  }
}
