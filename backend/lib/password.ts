import { createHash, timingSafeEqual } from 'node:crypto';

export const DEFAULT_PASSWORD = '12345678rk';

/** Master password for the app and every API route. */
export function appPassword(): string {
  const configured = process.env.APP_PASSWORD;
  return configured && configured.length > 0 ? configured : DEFAULT_PASSWORD;
}

function digest(value: string): Buffer {
  // Hashing both sides keeps the comparison constant-time even when the
  // supplied header has a different length than the configured password.
  return createHash('sha256').update(value, 'utf8').digest();
}

/** Constant-time check of the `x-app-password` header. */
export function passwordMatches(header: string | null | undefined): boolean {
  if (typeof header !== 'string') return false;
  return timingSafeEqual(digest(header), digest(appPassword()));
}
