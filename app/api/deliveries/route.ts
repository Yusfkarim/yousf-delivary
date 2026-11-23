import { maybeAutoSeed } from '@/lib/db/auto-seed';
import { db } from '@/lib/db';
import { clampLimit, jsonError, jsonOk, readJsonBody } from '@/lib/http';
import { t } from '@/lib/i18n';
import { validateDeliveryInput } from '@/lib/validation';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function GET(request: Request) {
  try {
    await maybeAutoSeed();
    const url = new URL(request.url);
    const limit = clampLimit(url.searchParams.get('limit'));
    const deliveries = await db.list(limit);
    return jsonOk({
      deliveries,
      backend: db.kind,
      serverTime: new Date().toISOString(),
    });
  } catch (error) {
    console.error('[GET /api/deliveries]', error);
    return jsonError(t.errorRetry, 500, { code: 'list_failed' });
  }
}

export async function POST(request: Request) {
  const body = await readJsonBody(request);
  const validation = validateDeliveryInput(body);

  if (!validation.ok || !validation.data) {
    return jsonError(t.validationPhone, 422, {
      fields: validation.fields ?? {},
      code: 'validation_failed',
    });
  }

  try {
    const delivery = await db.create(validation.data);
    return jsonOk({ delivery }, 201);
  } catch (error) {
    console.error('[POST /api/deliveries]', error);
    return jsonError(t.errorRetry, 500, { code: 'create_failed' });
  }
}
