/**
 * Iraqi / Kurdish phone number helpers.
 *
 * Handles everything a driver may paste:
 *   0750 123 4567
 *   +964 750 123 4567
 *   00964 750 123 4567
 *   9647501234567
 * All of them normalise to E.164: +9647501234567
 */

const IRAQ_DIAL_CODE = '964';

export function digitsOnly(value: string): string {
  return value.replace(/[^\d]/g, '');
}

/**
 * Normalise any loosely typed phone number to E.164.
 * Returns null when the number cannot be interpreted.
 */
export function normalizePhone(raw: string | null | undefined): string | null {
  if (!raw) return null;
  let value = String(raw).trim();
  if (!value) return null;

  // Convert Arabic-Indic / Persian digits to latin digits.
  value = value
    .replace(/[\u0660-\u0669]/g, (d) => String(d.charCodeAt(0) - 0x0660))
    .replace(/[\u06f0-\u06f9]/g, (d) => String(d.charCodeAt(0) - 0x06f0));

  value = value.replace(/(?:\s|-|\(|\)|\.)/g, '');
  if (!/^[+0-9]+$/.test(value)) return null;

  let digits = digitsOnly(value);
  if (!digits) return null;

  if (value.startsWith('+')) {
    digits = digits;
  } else if (digits.startsWith('00')) {
    digits = digits.slice(2);
  } else if (digits.startsWith('0')) {
    // local Iraqi format: 07501234567
    digits = IRAQ_DIAL_CODE + digits.replace(/^0+/, '');
  } else if (digits.startsWith('964')) {
    // already international
  } else if (digits.length === 10 && digits.startsWith('7')) {
    // 7501234567 -> Iraqi mobile without trunk code
    digits = IRAQ_DIAL_CODE + digits;
  }

  if (digits.length < 8 || digits.length > 15) return null;
  return `+${digits}`;
}

/** True when the number belongs to Iraq (+964). */
export function isIraqiPhone(e164: string): boolean {
  return digitsOnly(e164).startsWith(IRAQ_DIAL_CODE);
}

/** Dial-safe representation, e.g. +9647501234567 */
export function toTelHref(e164: string): string {
  return `tel:${e164}`;
}

/** wa.me needs digits only, no plus sign: 9647501234567 */
export function toWhatsAppNumber(e164: string): string {
  return digitsOnly(e164);
}

/**
 * Pretty, local-looking display: 0750 123 4567 for Iraqi numbers,
 * +964 750 123 4567 for international ones.
 */
export function formatPhone(e164: string | null | undefined): string {
  if (!e164) return '';
  const digits = digitsOnly(e164);
  if (digits.startsWith(IRAQ_DIAL_CODE) && digits.length === 13) {
    const local = `0${digits.slice(3)}`;
    return `${local.slice(0, 4)} ${local.slice(4, 7)} ${local.slice(7)}`;
  }
  if (digits.startsWith(IRAQ_DIAL_CODE)) {
    return `+${IRAQ_DIAL_CODE} ${digits.slice(3)}`;
  }
  return `+${digits}`;
}

export function toWhatsAppHref(e164: string, text?: string): string {
  const number = toWhatsAppNumber(e164);
  const base = `https://wa.me/${number}`;
  if (!text) return base;
  return `${base}?text=${encodeURIComponent(text)}`;
}

export function toViberHref(e164: string): string {
  const number = digitsOnly(e164);
  return `viber://chat?number=%2B${number}`;
}

/** A human friendly, unambiguous international format: +964 750 123 4567 */
export function formatInternational(e164: string | null | undefined): string {
  if (!e164) return '';
  const digits = digitsOnly(e164);
  const grouped = digits.replace(/(\d{3})(\d{3})(\d{3})(\d+)/, '$1 $2 $3 $4');
  return `+${grouped}`;
}

/** Best-effort check that a phone number list has at least one valid entry. */
export function firstValidPhone(...candidates: Array<string | null | undefined>): string | null {
  for (const candidate of candidates) {
    const normalised = normalizePhone(candidate);
    if (normalised) return normalised;
  }
  return null;
}
