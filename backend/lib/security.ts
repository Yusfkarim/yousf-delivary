import { createHmac, timingSafeEqual } from 'node:crypto';

/**
 * Request hardening for the delivery API.
 *
 * Three layers on top of the constant-time password check:
 *  1. a per-IP rate limit (a phone cannot legitimately send more than a few
 *     requests a second, a password guesser can),
 *  2. a short lockout after repeated wrong passwords,
 *  3. an optional HMAC signature (`x-app-signature`) so a leaked password alone
 *     is not enough: the caller must sign the method, path, timestamp and body
 *     with the server secret.
 */

const WINDOW_MS = 60_000;
const MAX_REQUESTS = 150;
const MAX_FAILURES = 8;
const LOCKOUT_MS = 10 * 60_000;
const SIGNATURE_TOLERANCE_MS = 5 * 60_000;

type Bucket = { hits: number; resetAt: number; failures: number; lockedUntil: number };

const buckets = new Map<string, Bucket>();

export function clientIp(request: Request): string {
  const forwarded = request.headers.get('x-forwarded-for');
  if (forwarded) return forwarded.split(',')[0]!.trim();
  return request.headers.get('x-real-ip') ?? 'unknown';
}

function bucketFor(ip: string, now: number): Bucket {
  const existing = buckets.get(ip);
  if (existing && existing.resetAt > now) return existing;
  const fresh: Bucket = { hits: 0, resetAt: now + WINDOW_MS, failures: 0, lockedUntil: 0 };
  buckets.set(ip, fresh);
  if (buckets.size > 5_000) {
    // keep the map bounded — a handful of drivers, not a botnet
    for (const [key, value] of buckets) {
      if (value.resetAt < now) buckets.delete(key);
    }
  }
  return fresh;
}

export type RateVerdict = { allowed: true } | { allowed: false; retryAfterSeconds: number; reason: 'locked' | 'burst' };

export function checkRate(ip: string): RateVerdict {
  const now = Date.now();
  const bucket = bucketFor(ip, now);
  if (bucket.lockedUntil > now) {
    return { allowed: false, retryAfterSeconds: Math.ceil((bucket.lockedUntil - now) / 1000), reason: 'locked' };
  }
  bucket.hits += 1;
  if (bucket.hits > MAX_REQUESTS) {
    return { allowed: false, retryAfterSeconds: Math.ceil((bucket.resetAt - now) / 1000), reason: 'burst' };
  }
  return { allowed: true };
}

export function noteFailure(ip: string): void {
  const now = Date.now();
  const bucket = bucketFor(ip, now);
  bucket.failures += 1;
  if (bucket.failures >= MAX_FAILURES) {
    bucket.lockedUntil = now + LOCKOUT_MS;
    bucket.failures = 0;
  }
}

export function noteSuccess(ip: string): void {
  const bucket = buckets.get(ip);
  if (bucket) bucket.failures = 0;
}

function signingSecret(): string | null {
  const secret = process.env.APP_SIGNING_SECRET;
  return secret && secret.length >= 16 ? secret : null;
}

/**
 * Verifies `x-app-signature` when the client sends it. Unsigned clients are still
 * accepted (the app on the driver's phone may not be updated yet), but a *wrong*
 * signature is refused — that is what makes a stolen password useless once the
 * client starts signing.
 */
export function signatureIsValid(request: Request, body: string): boolean | null {
  const header = request.headers.get('x-app-signature');
  if (!header) return null;
  const secret = signingSecret();
  if (!secret) return null;

  const stamp = Number(request.headers.get('x-app-timestamp') ?? '0');
  if (!Number.isFinite(stamp) || Math.abs(Date.now() - stamp) > SIGNATURE_TOLERANCE_MS) return false;

  const url = new URL(request.url);
  const payload = [request.method.toUpperCase(), url.pathname, String(stamp), body].join('\n');
  const expected = createHmac('sha256', secret).update(payload, 'utf8').digest('hex');
  const given = header.toLowerCase().replace(/^sha256=/, '');
  if (given.length !== expected.length) return false;
  return timingSafeEqual(Buffer.from(given, 'utf8'), Buffer.from(expected, 'utf8'));
}
