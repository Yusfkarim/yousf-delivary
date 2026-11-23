import { jsonError, jsonOk, readJsonBody } from '@/lib/http';
import { PIN_COOKIE, PIN_MAX_AGE_SECONDS, hashPin, isPinEnabled } from '@/lib/pin';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function POST(request: Request) {
  if (!isPinEnabled()) return jsonOk({ ok: true, locked: false });

  const body = (await readJsonBody(request)) as { pin?: unknown } | null;
  const attempt = typeof body?.pin === 'string' ? body.pin.replace(/\D/g, '') : '';

  if (!attempt) return jsonError('کۆدی چوونەژوورەوە بنووسە', 422, { code: 'pin_required' });

  const pin = process.env.APP_PIN?.trim() ?? '';
  if (attempt !== pin) {
    return jsonError('کۆدەکە هەڵەیە، دووبارە هەوڵبدەرەوە', 401, { code: 'pin_invalid' });
  }

  const response = jsonOk({ ok: true, locked: true });
  response.cookies.set({
    name: PIN_COOKIE,
    value: await hashPin(pin),
    httpOnly: true,
    sameSite: 'lax',
    path: '/',
    maxAge: PIN_MAX_AGE_SECONDS,
    secure: process.env.NODE_ENV === 'production',
  });
  return response;
}

export async function GET() {
  return jsonOk({ locked: isPinEnabled() });
}
