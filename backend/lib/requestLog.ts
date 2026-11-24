/**
 * Request journal for field diagnostics.
 *
 * Every hit on a customer route is written to `request_log` in Supabase
 * (service role, bypasses RLS). When a phone claims "404" but the route works
 * from elsewhere, this table tells us whether the request reached the function
 * at all — and with which User-Agent.
 *
 * Enabled by `DELIVERY_LOG_REQUESTS=1`; never throws.
 */
const TABLE = 'request_log';

function config(): { url: string; key: string } | null {
  if (process.env.DELIVERY_LOG_REQUESTS !== '1') return null;
  const url =
    (process.env.DELIVERY_SUPABASE_URL ?? process.env.SUPABASE_URL ?? '').trim().replace(/\/+$/, '');
  const key = (
    process.env.DELIVERY_SUPABASE_SERVICE_KEY ??
    process.env.SUPABASE_SERVICE_ROLE_KEY ??
    process.env.SUPABASE_SECRET_KEY ??
    ''
  ).trim();
  if (!url || !key) return null;
  return { url, key };
}

function clientIp(request: Request): string | null {
  const forwarded = request.headers.get('x-forwarded-for');
  if (forwarded) return forwarded.split(',')[0]?.trim() ?? null;
  return request.headers.get('x-real-ip');
}

export async function logRequest(request: Request, route: string, note?: string): Promise<void> {
  const cfg = config();
  if (!cfg) return;

  const row = {
    route,
    method: request.method,
    ua: request.headers.get('user-agent'),
    ip: clientIp(request),
    note: note ?? null,
  };

  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 2500);
    await fetch(`${cfg.url}/rest/v1/${TABLE}`, {
      method: 'POST',
      headers: {
        apikey: cfg.key,
        Authorization: `Bearer ${cfg.key}`,
        'Content-Type': 'application/json',
        Prefer: 'return=minimal',
      },
      body: JSON.stringify(row),
      signal: controller.signal,
      cache: 'no-store',
    }).finally(() => clearTimeout(timer));
  } catch {
    // Diagnostics must never break the API.
  }
}
