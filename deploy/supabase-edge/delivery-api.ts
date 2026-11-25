// Yousif Delivery — API relay on Supabase Edge Functions.
//
// Some mobile networks answer the Vercel API with a 404 (ISP interception),
// while `*.supabase.co` is always reachable. This function forwards every
// request to the real API server-side and returns its answer untouched, so the
// Android app can use the very same contract from a second host:
//
//   https://<ref>.supabase.co/functions/v1/delivery-api/api/customers
//
// The `x-app-password` header is passed through; no key is stored here.

const UPSTREAM = 'https://yousif-delivery-api.vercel.app';

const CORS: Record<string, string> = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, x-app-password',
  'Cache-Control': 'no-store, max-age=0',
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS, 'Content-Type': 'application/json; charset=utf-8' },
  });
}

Deno.serve(async (request: Request): Promise<Response> => {
  const url = new URL(request.url);
  const path = url.pathname.replace(/^\/functions\/v1\/[^/]+/, '');
  if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });

  if (path === '' || path === '/' || path === '/api/health') {
    // Liveness of the relay itself; a real health call is forwarded below.
    if (path !== '/api/health') {
      return json({ ok: true, service: 'yousif-delivery-api', transport: 'supabase-relay', upstream: UPSTREAM });
    }
  }

  const headers = new Headers();
  const password = request.headers.get('x-app-password');
  if (password) headers.set('x-app-password', password);
  headers.set('Accept', 'application/json');

  let body: string | undefined;
  if (request.method !== 'GET' && request.method !== 'DELETE') {
    body = await request.text();
    headers.set('Content-Type', 'application/json');
  }

  try {
    const upstream = await fetch(`${UPSTREAM}${path}${url.search}`, {
      method: request.method,
      headers,
      body,
      redirect: 'manual',
    });
    const text = await upstream.text();
    return new Response(text, {
      status: upstream.status,
      headers: { ...CORS, 'Content-Type': 'application/json; charset=utf-8' },
    });
  } catch (error) {
    return json(
      { error: 'upstream_unreachable', detail: error instanceof Error ? error.message : 'unknown' },
      502,
    );
  }
});
