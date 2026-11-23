import { isValidLat, isValidLng } from './geo';
import { normalizePhone } from './phone';
import {
  CURRENCIES,
  DELIVERY_STATUSES,
  MESSAGING_CHANNELS,
  type Currency,
  type DeliveryInput,
  type DeliveryStatus,
  type MessagingChannel,
  type ValidationResult,
} from './types';
import { validationMessages } from './i18n';

const MAX_NAME = 120;
const MAX_TEXT = 400;
const MAX_NOTE = 800;

function clampString(value: unknown, max = MAX_TEXT): string {
  if (typeof value !== 'string') return '';
  return value.trim().slice(0, max);
}

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value.replace(/,/g, '').replace(/[^\d.-]/g, ''));
    if (Number.isFinite(parsed)) return parsed;
  }
  return fallback;
}

export function isCurrency(value: unknown): value is Currency {
  return typeof value === 'string' && (CURRENCIES as readonly string[]).includes(value);
}

export function isDeliveryStatus(value: unknown): value is DeliveryStatus {
  return typeof value === 'string' && (DELIVERY_STATUSES as readonly string[]).includes(value);
}

export function parseChannels(value: unknown): MessagingChannel[] {
  if (!Array.isArray(value)) return [];
  const unique = new Set<MessagingChannel>();
  for (const item of value) {
    if (typeof item === 'string' && (MESSAGING_CHANNELS as readonly string[]).includes(item)) {
      unique.add(item as MessagingChannel);
    }
  }
  return Array.from(unique);
}

/** Normalise a list of phone strings, dropping empties and duplicates. */
export function parsePhoneList(value: unknown, primary?: string | null): {
  phones: string[];
  invalid: string[];
} {
  const phones: string[] = [];
  const invalid: string[] = [];
  if (!Array.isArray(value)) return { phones, invalid };

  for (const item of value) {
    if (typeof item !== 'string' || !item.trim()) continue;
    const normalised = normalizePhone(item);
    if (!normalised) {
      invalid.push(item);
      continue;
    }
    if (primary && normalised === primary) continue;
    if (!phones.includes(normalised)) phones.push(normalised);
  }
  return { phones, invalid };
}

/**
 * Validate a create/update payload for a delivery.
 * Field names are returned so the UI can highlight inputs.
 */
export function validateDeliveryInput(body: unknown): ValidationResult<DeliveryInput> {
  const fields: Record<string, string> = {};
  const raw = (body ?? {}) as Record<string, unknown>;

  const name = clampString(raw.name, MAX_NAME);
  if (name.length < 2) fields.name = validationMessages.name;

  const normalisedPhone = normalizePhone(typeof raw.primaryPhone === 'string' ? raw.primaryPhone : '');
  if (!normalisedPhone) fields.primaryPhone = validationMessages.phone;

  const { phones, invalid } = parsePhoneList(raw.extraPhones, normalisedPhone);
  if (invalid.length > 0) fields.extraPhones = validationMessages.extraPhone;

  let latitude: number | null = null;
  let longitude: number | null = null;
  const rawLat = raw.latitude;
  const rawLng = raw.longitude;
  const hasLat = rawLat !== null && rawLat !== undefined && rawLat !== '';
  const hasLng = rawLng !== null && rawLng !== undefined && rawLng !== '';
  if (hasLat || hasLng) {
    const lat = toNumber(rawLat, Number.NaN);
    const lng = toNumber(rawLng, Number.NaN);
    if (!isValidLat(lat) || !isValidLng(lng)) {
      fields.latitude = validationMessages.coords;
    } else {
      latitude = Math.round(lat * 1e6) / 1e6;
      longitude = Math.round(lng * 1e6) / 1e6;
    }
  }

  const currency = isCurrency(raw.currency) ? raw.currency : 'IQD';
  const price = Math.max(0, toNumber(raw.price, 0));
  const deliveryFee = Math.max(0, toNumber(raw.deliveryFee, 0));
  if (!Number.isFinite(price) || price < 0) fields.price = validationMessages.price;
  if (!Number.isFinite(deliveryFee) || deliveryFee < 0) fields.deliveryFee = validationMessages.fee;

  const status = isDeliveryStatus(raw.status) ? raw.status : 'pending';

  if (Object.keys(fields).length > 0 || !normalisedPhone) {
    return { ok: false, fields };
  }

  const customerId =
    typeof raw.customerId === 'string' && /^[0-9a-zA-Z-]{6,64}$/.test(raw.customerId)
      ? raw.customerId
      : undefined;

  return {
    ok: true,
    data: {
      customerId,
      name,
      primaryPhone: normalisedPhone,
      extraPhones: phones,
      latitude,
      longitude,
      address: clampString(raw.address),
      landmark: clampString(raw.landmark),
      channels: parseChannels(raw.channels),
      customerNotes: clampString(raw.customerNotes, MAX_NOTE),
      itemNote: clampString(raw.itemNote, MAX_NOTE),
      price: Math.round(price * 100) / 100,
      currency,
      deliveryFee: Math.round(deliveryFee * 100) / 100,
      status,
    },
  };
}

/** Validate a partial patch (used by PATCH endpoints). */
export function validateDeliveryPatch(body: unknown): ValidationResult<Record<string, unknown>> {
  const fields: Record<string, string> = {};
  const raw = (body ?? {}) as Record<string, unknown>;
  const patch: Record<string, unknown> = {};

  if ('name' in raw) {
    const name = clampString(raw.name, MAX_NAME);
    if (name.length < 2) fields.name = validationMessages.name;
    else patch.name = name;
  }

  if ('primaryPhone' in raw) {
    const phone = normalizePhone(typeof raw.primaryPhone === 'string' ? raw.primaryPhone : '');
    if (!phone) fields.primaryPhone = validationMessages.phone;
    else patch.primaryPhone = phone;
  }

  if ('extraPhones' in raw) {
    const primary =
      typeof patch.primaryPhone === 'string'
        ? patch.primaryPhone
        : normalizePhone(typeof raw.primaryPhone === 'string' ? raw.primaryPhone : '');
    const { phones, invalid } = parsePhoneList(raw.extraPhones, primary);
    if (invalid.length > 0) fields.extraPhones = validationMessages.extraPhone;
    else patch.extraPhones = phones;
  }

  if ('latitude' in raw || 'longitude' in raw) {
    if (raw.latitude === null && raw.longitude === null) {
      patch.latitude = null;
      patch.longitude = null;
    } else {
      const lat = toNumber(raw.latitude, Number.NaN);
      const lng = toNumber(raw.longitude, Number.NaN);
      if (!isValidLat(lat) || !isValidLng(lng)) {
        fields.latitude = validationMessages.coords;
      } else {
        patch.latitude = lat;
        patch.longitude = lng;
      }
    }
  }

  if ('address' in raw) patch.address = clampString(raw.address);
  if ('landmark' in raw) patch.landmark = clampString(raw.landmark);
  if ('channels' in raw) patch.channels = parseChannels(raw.channels);
  if ('customerNotes' in raw) patch.customerNotes = clampString(raw.customerNotes, MAX_NOTE);
  if ('itemNote' in raw) patch.itemNote = clampString(raw.itemNote, MAX_NOTE);

  if ('price' in raw) {
    const price = toNumber(raw.price, Number.NaN);
    if (!Number.isFinite(price) || price < 0) fields.price = validationMessages.price;
    else patch.price = Math.round(price * 100) / 100;
  }

  if ('deliveryFee' in raw) {
    const fee = toNumber(raw.deliveryFee, Number.NaN);
    if (!Number.isFinite(fee) || fee < 0) fields.deliveryFee = validationMessages.fee;
    else patch.deliveryFee = Math.round(fee * 100) / 100;
  }

  if ('currency' in raw) {
    if (!isCurrency(raw.currency)) fields.currency = validationMessages.price;
    else patch.currency = raw.currency;
  }

  if ('status' in raw) {
    if (!isDeliveryStatus(raw.status)) fields.status = 'دۆخی نادروست';
    else patch.status = raw.status;
  }

  if (Object.keys(fields).length > 0) return { ok: false, fields };
  if (Object.keys(patch).length === 0) return { ok: false, fields: { _: 'هیچ گۆڕانکارییەک نییە' } };
  return { ok: true, data: patch };
}

export function validateStatusBody(body: unknown): DeliveryStatus | null {
  const raw = (body ?? {}) as Record<string, unknown>;
  return isDeliveryStatus(raw.status) ? raw.status : null;
}
