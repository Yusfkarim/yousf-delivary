import { isConfigured } from '@/lib/db';
import { json, preflight } from '@/lib/http';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

/** Public liveness probe — exposes no customer data, only configuration state. */
export async function GET() {
  return json({
    ok: true,
    service: 'yousif-delivery-api',
    databaseConfigured: isConfigured(),
    serverTime: Date.now(),
  });
}

export async function OPTIONS() {
  return preflight();
}
