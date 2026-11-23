/**
 * Optional PIN gate helpers. Works in both the Edge middleware and Node route
 * handlers because it only relies on Web Crypto.
 */
export const PIN_COOKIE = 'yd_session';
export const PIN_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

export async function hashPin(pin: string): Promise<string> {
  const data = new TextEncoder().encode(`yousif-delivery::${pin}`);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
}

export function isPinEnabled(): boolean {
  return Boolean(process.env.APP_PIN && process.env.APP_PIN.trim().length >= 4);
}

/** Public routes that must stay reachable while the app is locked. */
export function isPublicPath(pathname: string): boolean {
  return (
    pathname === '/unlock' ||
    pathname === '/api/unlock' ||
    pathname === '/manifest.webmanifest' ||
    pathname === '/sw.js' ||
    pathname === '/favicon.png' ||
    pathname === '/favicon.ico' ||
    pathname.startsWith('/icons/') ||
    pathname.startsWith('/_next/')
  );
}
