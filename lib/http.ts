import { NextResponse } from 'next/server';

export const NO_STORE_HEADERS = {
  'Cache-Control': 'no-store, no-cache, must-revalidate',
} as const;

export function jsonOk<T>(data: T, status = 200) {
  return NextResponse.json(data, { status, headers: NO_STORE_HEADERS });
}

export function jsonError(
  message: string,
  status = 400,
  extra?: { fields?: Record<string, string>; code?: string },
) {
  return NextResponse.json(
    {
      error: extra?.code ?? 'request_failed',
      message,
      ...(extra?.fields ? { fields: extra.fields } : {}),
    },
    { status, headers: NO_STORE_HEADERS },
  );
}

export async function readJsonBody(request: Request): Promise<unknown> {
  try {
    return await request.json();
  } catch {
    return null;
  }
}

export function isValidId(id: string): boolean {
  return /^[0-9a-zA-Z-]{6,64}$/.test(id);
}

export function clampLimit(value: string | null | undefined, fallback = 500): number {
  if (value === null || value === undefined || value.trim() === '') return fallback;
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.min(1000, Math.max(1, Math.round(parsed)));
}
