/** Extra phone number attached to a customer. Stored inside the JSONB column. */
export interface ExtraPhone {
  number: string;
  has_whatsapp: boolean;
  has_viber: boolean;
}

/** Row shape exactly as it lives in the `customers` table. */
export interface CustomerRow {
  id: string;
  name: string;
  primary_phone: string;
  has_whatsapp: boolean;
  has_viber: boolean;
  extra_phones: ExtraPhone[] | string | null;
  latitude: number | string;
  longitude: number | string;
  address_note: string | null;
  created_at: number | string;
  updated_at: number | string;
}

/** JSON shape returned to the Android client (camelCase). */
export interface CustomerPayload {
  id: string;
  name: string;
  primaryPhone: string;
  hasWhatsapp: boolean;
  hasViber: boolean;
  extraPhones: ExtraPhone[];
  latitude: number;
  longitude: number;
  addressNote: string;
  createdAt: number;
  updatedAt: number;
}

const UUID_RE = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

export function isUuid(value: unknown): value is string {
  return typeof value === 'string' && UUID_RE.test(value);
}

function num(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function bool(value: unknown, fallback = false): boolean {
  if (typeof value === 'boolean') return value;
  if (value === 'true') return true;
  if (value === 'false') return false;
  return fallback;
}

export function parseExtraPhones(input: unknown): ExtraPhone[] {
  if (!Array.isArray(input)) return [];
  const out: ExtraPhone[] = [];
  for (const item of input) {
    if (!item || typeof item !== 'object') continue;
    const record = item as Record<string, unknown>;
    const number = typeof record.number === 'string' ? record.number.trim() : '';
    if (!number) continue;
    out.push({
      number,
      has_whatsapp: bool(record.has_whatsapp ?? record.hasWhatsapp),
      has_viber: bool(record.has_viber ?? record.hasViber),
    });
  }
  return out.slice(0, 25);
}

export interface ParsedCustomer {
  id: string;
  name: string;
  primaryPhone: string;
  hasWhatsapp: boolean;
  hasViber: boolean;
  extraPhones: ExtraPhone[];
  latitude: number;
  longitude: number;
  addressNote: string;
  createdAt: number;
  updatedAt: number;
}

/** Validates an incoming body. Returns either the clean record or field errors. */
export function parseCustomer(
  body: unknown,
  options: { id?: string; partial?: boolean } = {},
): { ok: true; value: ParsedCustomer } | { ok: false; errors: Record<string, string> } {
  const errors: Record<string, string> = {};
  const record = (body ?? {}) as Record<string, unknown>;

  const id = options.id ?? (isUuid(record.id) ? (record.id as string) : '');
  if (!isUuid(id)) errors.id = 'id must be a UUID';

  const name = typeof record.name === 'string' ? record.name.trim() : '';
  if (!name) errors.name = 'name is required';
  if (name.length > 120) errors.name = 'name is too long';

  const primaryPhone = typeof record.primaryPhone === 'string'
    ? record.primaryPhone.trim()
    : typeof record.primary_phone === 'string'
      ? String(record.primary_phone).trim()
      : '';
  if (!primaryPhone) errors.primaryPhone = 'primaryPhone is required';
  if (primaryPhone.length > 40) errors.primaryPhone = 'primaryPhone is too long';

  const latitude = num(record.latitude);
  const longitude = num(record.longitude);
  if (latitude === null || latitude < -90 || latitude > 90) errors.latitude = 'latitude must be between -90 and 90';
  if (longitude === null || longitude < -180 || longitude > 180) errors.longitude = 'longitude must be between -180 and 180';

  const addressNote = typeof record.addressNote === 'string' ? record.addressNote.slice(0, 500) : '';
  const now = Date.now();
  const createdAt = num(record.createdAt) ?? now;
  const updatedAt = num(record.updatedAt) ?? now;

  if (Object.keys(errors).length > 0) return { ok: false, errors };

  return {
    ok: true,
    value: {
      id,
      name,
      primaryPhone,
      hasWhatsapp: bool(record.hasWhatsapp ?? record.has_whatsapp),
      hasViber: bool(record.hasViber ?? record.has_viber),
      extraPhones: parseExtraPhones(record.extraPhones ?? record.extra_phones),
      latitude: latitude as number,
      longitude: longitude as number,
      addressNote,
      createdAt,
      updatedAt,
    },
  };
}

export function rowToPayload(row: CustomerRow): CustomerPayload {
  let extraPhones: ExtraPhone[] = [];
  const raw = row.extra_phones;
  if (Array.isArray(raw)) {
    extraPhones = parseExtraPhones(raw);
  } else if (typeof raw === 'string' && raw.trim() !== '') {
    try {
      extraPhones = parseExtraPhones(JSON.parse(raw));
    } catch {
      extraPhones = [];
    }
  }
  return {
    id: row.id,
    name: row.name,
    primaryPhone: row.primary_phone,
    hasWhatsapp: row.has_whatsapp === true,
    hasViber: row.has_viber === true,
    extraPhones,
    latitude: Number(row.latitude),
    longitude: Number(row.longitude),
    addressNote: row.address_note ?? '',
    createdAt: Number(row.created_at),
    updatedAt: Number(row.updated_at),
  };
}
