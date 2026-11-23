import { NextResponse, type NextRequest } from 'next/server';
import { PIN_COOKIE, hashPin, isPinEnabled, isPublicPath } from '@/lib/pin';

/**
 * Optional numeric PIN gate (`APP_PIN`). When unset — the default — every
 * request passes through untouched.
 */
export async function middleware(request: NextRequest) {
  if (!isPinEnabled()) return NextResponse.next();

  const { pathname, search } = request.nextUrl;
  if (isPublicPath(pathname)) return NextResponse.next();

  const pin = process.env.APP_PIN?.trim() ?? '';
  const expected = await hashPin(pin);
  const session = request.cookies.get(PIN_COOKIE)?.value;

  if (session && session === expected) return NextResponse.next();

  if (pathname.startsWith('/api/')) {
    return NextResponse.json(
      { error: 'locked', message: 'کۆدی چوونەژوورەوە پێویستە' },
      { status: 401, headers: { 'Cache-Control': 'no-store' } },
    );
  }

  const url = request.nextUrl.clone();
  url.pathname = '/unlock';
  url.search = pathname === '/' ? '' : `?next=${encodeURIComponent(`${pathname}${search}`)}`;
  return NextResponse.redirect(url);
}

export const config = {
  matcher: ['/((?!_next/static|_next/image).*)'],
};
