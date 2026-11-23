import { db } from '@/lib/db';
import { jsonError, jsonOk } from '@/lib/http';
import { t } from '@/lib/i18n';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function POST() {
  try {
    const existing = await db.count();
    if (existing > 0) {
      return jsonOk({ inserted: 0, skipped: true });
    }
    const inserted = await db.seedDemo();
    return jsonOk({ inserted, skipped: false });
  } catch (error) {
    console.error('[POST /api/demo]', error);
    return jsonError(t.errorRetry, 500, { code: 'seed_failed' });
  }
}
