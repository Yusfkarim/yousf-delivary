import { NextResponse } from 'next/server';
import { describeError, getStore, isConfigured, type CustomerStore } from './db';
import { passwordMatches } from './password';

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
