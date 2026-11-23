import { db } from '@/lib/db';
import { jsonError, jsonOk } from '@/lib/http';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function GET() {
  try {
    const health = await db.health();
    const count = health.ok ? await db.count() : 0;
    return jsonOk({
      ok: health.ok,
      backend: health.backend,
      schemaReady: health.schemaReady,
      message: health.message,
      count,
      serverTime: new Date().toISOString(),
    });
  } catch (error) {
    return jsonError(
      error instanceof Error ? error.message : 'health check failed',
      500,
      { code: 'health_failed' },
    );
  }
}
