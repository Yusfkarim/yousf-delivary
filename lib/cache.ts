import type { Currency, Delivery, DeliveryInput, DeliveryPatch, DeliveryStatus } from './types';

export const CACHE_KEYS = {
  deliveries: 'yd.deliveries.v1',
  settings: 'yd.settings.v1',
  outbox: 'yd.outbox.v1',
  location: 'yd.location.v1',
  cacheStamp: 'yd.cacheStamp.v1',
} as const;

function hasWindow(): boolean {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';
}

export function readCache<T>(key: string, fallback: T): T {
  if (!hasWindow()) return fallback;
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export function writeCache(key: string, value: unknown): void {
  if (!hasWindow()) return;
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // quota exceeded / private mode — silently ignore, the app still works online
  }
}

export function removeCache(key: string): void {
  if (!hasWindow()) return;
  try {
    window.localStorage.removeItem(key);
  } catch {
    /* ignore */
  }
}

export function clearAppCache(): void {
  for (const key of Object.values(CACHE_KEYS)) removeCache(key);
  if (!hasWindow()) return;
  try {
    window.localStorage.removeItem('yd.draft.v1');
  } catch {
    /* ignore */
  }
}

export function cacheSizeBytes(): number {
  if (!hasWindow()) return 0;
  let total = 0;
  for (const key of Object.values(CACHE_KEYS)) {
    const raw = window.localStorage.getItem(key);
    if (raw) total += raw.length * 2;
  }
  return total;
}

export function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 KB';
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export interface AppSettings {
  driverName: string;
  kurdishDigits: boolean;
  defaultCurrency: Currency;
  defaultFee: number;
}

export const defaultSettings: AppSettings = {
  driverName: 'یوسف',
  kurdishDigits: true,
  defaultCurrency: process.env.NEXT_PUBLIC_DEFAULT_CURRENCY === 'USD' ? 'USD' : 'IQD',
  defaultFee: 3000,
};

export function readSettings(): AppSettings {
  const stored = readCache<Partial<AppSettings>>(CACHE_KEYS.settings, {});
  return {
    driverName: typeof stored.driverName === 'string' && stored.driverName.trim() ? stored.driverName : defaultSettings.driverName,
    kurdishDigits: typeof stored.kurdishDigits === 'boolean' ? stored.kurdishDigits : defaultSettings.kurdishDigits,
    defaultCurrency: stored.defaultCurrency === 'USD' ? 'USD' : defaultSettings.defaultCurrency,
    defaultFee: typeof stored.defaultFee === 'number' && stored.defaultFee >= 0 ? stored.defaultFee : defaultSettings.defaultFee,
  };
}

export type OutboxItem =
  | { id: string; type: 'create'; tempId: string; payload: DeliveryInput; createdAt: string }
  | { id: string; type: 'status'; deliveryId: string; status: DeliveryStatus; createdAt: string }
  | { id: string; type: 'update'; deliveryId: string; patch: DeliveryPatch; createdAt: string }
  | { id: string; type: 'delete'; deliveryId: string; createdAt: string };

export function readOutbox(): OutboxItem[] {
  const items = readCache<OutboxItem[]>(CACHE_KEYS.outbox, []);
  return Array.isArray(items) ? items.filter((item) => item && typeof item.id === 'string') : [];
}

export function writeOutbox(items: OutboxItem[]): void {
  writeCache(CACHE_KEYS.outbox, items);
}

export function readCachedDeliveries(): Delivery[] {
  const items = readCache<Delivery[]>(CACHE_KEYS.deliveries, []);
  return Array.isArray(items) ? items : [];
}

export function writeCachedDeliveries(deliveries: Delivery[]): void {
  writeCache(CACHE_KEYS.deliveries, deliveries);
  writeCache(CACHE_KEYS.cacheStamp, new Date().toISOString());
}

export function readCachedLocation(): { lat: number; lng: number; at: string } | null {
  const value = readCache<{ lat: number; lng: number; at: string } | null>(CACHE_KEYS.location, null);
  if (!value || typeof value.lat !== 'number' || typeof value.lng !== 'number') return null;
  return value;
}

export function writeCachedLocation(lat: number, lng: number): void {
  writeCache(CACHE_KEYS.location, { lat, lng, at: new Date().toISOString() });
}
