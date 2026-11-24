import { NextResponse } from 'next/server';
import { ensureSchema, getPool, isConfigured } from './db';
import { passwordMatches } from './password';

export type Pool = NonNullable<ReturnType<typeof getPool>>;

const CORS_HEADERS: Record<string, string> = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, x-app-password',
  'Cache-Control': 'no-store',
};

export function json(data: unknown, status = 200): NextResponse {
  return NextResponse.json(data, { status, headers: CORS_HEADERS });
}

export function preflight(): NextResponse {
  return new NextResponse(null, { status: 204, headers: CORS_HEADERS });
}

/** 401 unless the `x-app-password` header matches APP_PASSWORD (constant time). */
export function requirePassword(request: Request): NextResponse | null {
  if (passwordMatches(request.headers.get('x-app-password'))) return null;
  return json({ error: 'unauthorized', message: 'وشەی نهێنی هەڵەیە' }, 401);
}

/** Resolves a pool and makes sure the schema exists. */
export async function requireDatabase(): Promise<{ pool: Pool } | { response: NextResponse }> {
  if (!isConfigured()) {
    return {
      response: json(
        {
          error: 'database_not_configured',
          message: 'POSTGRES_URL دانەنراوە — داتابەیس بە پرۆژەکەوە ببەستەوە',
        },
        503,
      ),
    };
  }
  const pool = getPool();
  if (!pool) {
    return { response: json({ error: 'database_not_configured' }, 503) };
  }
  try {
    await ensureSchema(pool);
  } catch (error) {
    console.error('[db] schema init failed', error);
    return {
      response: json({ error: 'database_error', message: 'گرێدان بە داتابەیس سەرکەوتوو نەبوو' }, 503),
    };
  }
  return { pool };
}

export function unauthorized(): NextResponse {
  return json({ error: 'unauthorized' }, 401);
}
