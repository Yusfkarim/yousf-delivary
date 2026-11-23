import { CITY_SPEED_KMH, ROAD_FACTOR } from './geo';

const KURDISH_DIGITS = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];

export function toKurdishDigits(input: string | number): string {
  return String(input).replace(/\d/g, (digit) => KURDISH_DIGITS[Number(digit)]);
}

export function toLatinDigits(input: string): string {
  return input
    .replace(/[\u0660-\u0669]/g, (d) => String(d.charCodeAt(0) - 0x0660))
    .replace(/[\u06f0-\u06f9]/g, (d) => String(d.charCodeAt(0) - 0x06f0));
}

export function groupThousands(value: number): string {
  const rounded = Math.round(value);
  return rounded.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

const CURRENCY_SUFFIX: Record<string, string> = {
  IQD: 'د.ع',
  USD: '$',
};

export function formatMoney(
  value: number,
  currency: string = 'IQD',
  options: { kurdish?: boolean; compact?: boolean } = {},
): string {
  const { kurdish = true, compact = false } = options;
  const amount = Number.isFinite(value) ? value : 0;

  if (compact && Math.abs(amount) >= 1_000_000) {
    const millions = (amount / 1_000_000).toFixed(1).replace(/\.0$/, '');
    return kurdish ? `${toKurdishDigits(millions)} ملیۆن` : `${millions}M`;
  }
  if (compact && Math.abs(amount) >= 100_000) {
    const thousands = Math.round(amount / 1_000).toString();
    return kurdish ? `${toKurdishDigits(thousands)} هەزار` : `${thousands}K`;
  }

  const formatted = groupThousands(amount);
  const digits = kurdish ? toKurdishDigits(formatted) : formatted;

  if (currency === 'USD') return `$${digits}`;
  return `${digits} ${CURRENCY_SUFFIX.IQD}`;
}

export function formatCount(value: number, kurdish = true): string {
  const formatted = groupThousands(value);
  return kurdish ? toKurdishDigits(formatted) : formatted;
}

/** 350 → "٣٥٠ م" · 2400 → "٢.٤ کم" */
export function formatDistanceDistance(meters: number, kurdish = true): string {
  if (!Number.isFinite(meters) || meters < 0) {
    return kurdish ? '—' : '—';
  }
  if (meters < 1000) {
    const value = Math.max(0, Math.round(meters / 10) * 10);
    return kurdish ? `${toKurdishDigits(value)} م` : `${value} m`;
  }
  const km = meters / 1000;
  const text = km < 10 ? km.toFixed(1) : Math.round(km).toString();
  return kurdish ? `${toKurdishDigits(text)} کم` : `${text} km`;
}

/** Distance + ETA sentence used on delivery cards. */
export function formatDistanceSentence(meters: number, kurdish = true): string {
  return `${formatDistanceDistance(meters, kurdish)} دوورە لێتەوە`;
}

export function formatDuration(minutes: number, kurdish = true): string {
  const value = Math.max(0, Math.round(minutes));
  if (value < 60) {
    return kurdish ? `${toKurdishDigits(value)} خولەک` : `${value} min`;
  }
  const hours = Math.floor(value / 60);
  const rest = value % 60;
  if (kurdish) {
    return rest > 0
      ? `${toKurdishDigits(hours)} کاتژمێر و ${toKurdishDigits(rest)} خولەک`
      : `${toKurdishDigits(hours)} کاتژمێر`;
  }
  return rest > 0 ? `${hours}h ${rest}m` : `${hours}h`;
}

export function estimateEta(distanceKm: number): number {
  if (!Number.isFinite(distanceKm) || distanceKm <= 0) return 0;
  return Math.max(1, Math.round((distanceKm * ROAD_FACTOR * 60) / CITY_SPEED_KMH));
}

const KURDISH_MONTHS = [
  'کانوونی دووەم',
  'شوبات',
  'ئازار',
  'نیسان',
  'ئایار',
  'حوزەیران',
  'تەمموز',
  'ئاب',
  'ئەیلوول',
  'تشرینی یەکەم',
  'تشرینی دووەم',
  'کانوونی یەکەم',
];

const KURDISH_WEEKDAYS = ['یەکشەممە', 'دووشەممە', 'سێشەممە', 'چوارشەممە', 'پێنجشەممە', 'هەینی', 'شەممە'];

function asDate(value: string | number | Date): Date | null {
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function sameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
  );
}

/** "١٤:٠٥" */
export function formatTime(value: string | number | Date, kurdish = true): string {
  const date = asDate(value);
  if (!date) return '';
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  const text = `${hours}:${minutes}`;
  return kurdish ? toKurdishDigits(text) : text;
}

/** "٢٣ی ئازار ٢٠٢٦" */
export function formatDate(value: string | number | Date, kurdish = true): string {
  const date = asDate(value);
  if (!date) return '';
  const day = date.getDate();
  const month = KURDISH_MONTHS[date.getMonth()];
  const year = date.getFullYear();
  if (!kurdish) {
    return `${day} ${['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'][date.getMonth()]} ${year}`;
  }
  return `${toKurdishDigits(day)}ی ${month} ${toKurdishDigits(year)}`;
}

/** "٢٣ی ئازار ٢٠٢٦ · ١٤:٠٥" */
export function formatDateTime(value: string | number | Date, kurdish = true): string {
  const date = asDate(value);
  if (!date) return '';
  return `${formatDate(date, kurdish)} · ${formatTime(date, kurdish)}`;
}

export function relativeTime(value: string | number | Date, kurdish = true): string {
  const date = asDate(value);
  if (!date) return '';
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMinutes = Math.round(diffMs / 60000);

  if (diffMinutes < 1) return kurdish ? 'ئێستا' : 'just now';
  if (diffMinutes < 60) {
    return kurdish ? `${toKurdishDigits(diffMinutes)} خولەک لەمەوپێش` : `${diffMinutes} min ago`;
  }
  const diffHours = Math.round(diffMinutes / 60);
  if (diffHours < 24 && sameDay(date, now)) {
    return kurdish ? `${toKurdishDigits(diffHours)} کاتژمێر لەمەوپێش` : `${diffHours}h ago`;
  }

  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (sameDay(date, yesterday)) {
    return kurdish ? `دوێنێ ${formatTime(date, kurdish)}` : `yesterday ${formatTime(date, false)}`;
  }

  return formatDate(date, kurdish);
}

export function weekdayName(value: string | number | Date): string {
  const date = asDate(value);
  if (!date) return '';
  return KURDISH_WEEKDAYS[date.getDay()];
}

export function toDateTimeLocalValue(date: Date = new Date()): string {
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function startOfToday(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

export function isToday(value: string | number | Date): boolean {
  const date = asDate(value);
  if (!date) return false;
  return sameDay(date, new Date());
}
