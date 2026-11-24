import { NextResponse } from 'next/server';
import { describeError, getStore, isConfigured, type CustomerStore } from './db';
import { passwordMatches } from './password';
import { checkRate, clientIp, noteFailure, noteSuccess } from './security';

/**
 * The API is consumed by the Android app, not by a browser. CORS is therefore
 * closed unless an origin is explicitly allowed through APP_ORIGIN.
 */
const ALLOWED_ORIGIN = process.env.APP_ORIGIN ?? '';

const CORS_HEADERS: Record<string, string> = {
  ...(ALLOWED_ORIGIN ? { 'Access-Control-Allow-Origin': ALLOWED_ORIGIN, Vary: 'Origin' } : {}),
  'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, x-app-password, x-app-signature, x-app-timestamp',
  'Cache-Control': 'no-store, max-age=0',
  'X-Content-Type-Options': 'nosniff',
  'Referrer-Policy': 'no-referrer',
  'X-Frame-Options': 'DENY',
};

export function json(data: unknown, status = 200): NextResponse {
  return NextResponse.json(data, { status, headers: CORS_HEADERS });
}

export function preflight(): NextResponse {
  return new NextResponse(null, { status: 204, headers: CORS_HEADERS });
}

/**
 * 401 unless the `x-app-password` header matches APP_PASSWORD (constant time).
 * Rate-limited per IP, with a short lockout after repeated failures, so the
 * backend cannot be brute-forced from a laptop.
 */
export function requirePassword(request: Request): NextResponse | null {
  const ip = clientIp(request);
  const verdict = checkRate(ip);
  if (!verdict.allowed) {
    const response = json(
      {
        error: verdict.reason === 'locked' ? 'locked' : 'rate_limited',
        message:
          verdict.reason === 'locked'
            ? 'دوای چەند هەوڵێکی هەڵە، کەمێک بلۆک کرایەوە'
            : 'زۆر داواکاری نێردرا — تکایە کەمێک چاوەڕێ بکە',
        retryAfter: verdict.retryAfterSeconds,
      },
      429,
    );
    response.headers.set('Retry-After', String(verdict.retryAfterSeconds));
    return response;
  }

  if (passwordMatches(request.headers.get('x-app-password'))) {
    noteSuccess(ip);
    return null;
  }

  noteFailure(ip);
  return json({ error: 'unauthorized', message: 'وشەی نهێنی هەڵەیە' }, 401);
}

/** Resolves the active store (Postgres or Supabase REST). */
export async function requireDatabase(): Promise<
  { store: CustomerStore } | { response: NextResponse }
> {
  if (!isConfigured()) {
    return {
      response: json(
        {
          error: 'database_not_configured',
          message: 'داتابەیس بەستراوەتەوە نییە — لە Vercel داتابەیسێک (Postgres یان Supabase) ببەستەوە',
        },
        503,
      ),
    };
  }
  try {
    return { store: await getStore() };
  } catch (error) {
    console.error('[db] unavailable', error);
    return {
      response: json(
        {
          error: 'database_error',
          message: 'گرێدان بە داتابەیس سەرکەوتوو نەبوو',
          detail: describeError(error),
        },
        503,
      ),
    };
  }
}

export function unauthorized(): NextResponse {
  return json({ error: 'unauthorized' }, 401);
}
