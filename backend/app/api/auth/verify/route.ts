import type { NextRequest } from 'next/server';
import { json, preflight } from '@/lib/http';
import { passwordMatches } from '@/lib/password';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

/**
 * Verifies the master password.
 * Primary channel: the `x-app-password` header.
 * Fallback: a `{ "password": "..." }` JSON body (for curl / debugging).
 */
export async function POST(request: NextRequest) {
  const headerOk = passwordMatches(request.headers.get('x-app-password'));

  let bodyOk = false;
  if (!headerOk) {
    try {
      const body = (await request.json()) as { password?: unknown } | null;
      const candidate = body && typeof body.password === 'string' ? body.password : '';
      bodyOk = candidate.length > 0 && passwordMatches(candidate);
    } catch {
      bodyOk = false;
    }
  }

  if (!headerOk && !bodyOk) {
    return json({ error: 'unauthorized', message: 'وشەی نهێنی هەڵەیە' }, 401);
  }

  return json({ authenticated: true });
}

export async function OPTIONS() {
  return preflight();
}
