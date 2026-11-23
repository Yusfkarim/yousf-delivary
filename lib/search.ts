import { toLatinDigits } from './format';
import type { Delivery, LatLng, SortMode, StatusFilter } from './types';
import { haversineMeters } from './geo';
import { digitsOnly } from './phone';

const DIACRITICS = /[\u064B-\u065F\u0670\u06D6-\u06ED]/g;

/**
 * Normalises Kurdish/Arabic text for forgiving search:
 * unifies alef/yeh/kaf variants, drops diacritics, converts digits.
 */
export function normalizeSearch(value: string): string {
  return toLatinDigits(value.toLowerCase())
    .replace(DIACRITICS, '')
    .replace(/[\u0622\u0623\u0625]/g, '\u0627')
    .replace(/\u064A/g, '\u06CC')
    .replace(/\u0643/g, '\u06A9')
    .replace(/\u0629/g, '\u0647')
    .replace(/\u06D5/g, '\u0647')
    .replace(/[\u200B-\u200F]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

/** True when every character of `needle` appears in order inside `haystack`. */
export function isSubsequence(needle: string, haystack: string): boolean {
  if (!needle) return true;
  let index = 0;
  for (let i = 0; i < haystack.length && index < needle.length; i += 1) {
    if (haystack[i] === needle[index]) index += 1;
  }
  return index === needle.length;
}

export interface DeliverySearchIndex {
  name: string;
  phones: string[];
  phoneDigits: string[];
  address: string;
  landmark: string;
  item: string;
}

export function buildSearchIndex(delivery: Delivery): DeliverySearchIndex {
  const phones = [delivery.primaryPhone, ...delivery.extraPhones].filter(Boolean);
  return {
    name: normalizeSearch(delivery.name),
    phones: phones.map((phone) => normalizeSearch(phone)),
    phoneDigits: phones.map((phone) => digitsOnly(phone)),
    address: normalizeSearch(delivery.address),
    landmark: normalizeSearch(delivery.landmark),
    item: normalizeSearch(delivery.itemNote),
  };
}

/** Scores a delivery against a query — 0 means "no match". */
export function scoreMatch(index: DeliverySearchIndex, query: string): number {
  const tokens = normalizeSearch(query).split(' ').filter(Boolean);
  if (tokens.length === 0) return 0.01;

  const digits = digitsOnly(query);
  let score = 0;

  for (const token of tokens) {
    let tokenScore = 0;

    if (index.name.startsWith(token)) tokenScore = 100;
    else if (index.name.includes(token)) tokenScore = 80;
    else if (isSubsequence(token, index.name)) tokenScore = 30;

    if (index.phoneDigits.some((phone) => phone.includes(digits) && digits.length > 0)) {
      tokenScore = Math.max(tokenScore, 95);
    } else if (index.phones.some((phone) => phone.includes(token))) {
      tokenScore = Math.max(tokenScore, 60);
    }

    if (!tokenScore && (index.address.includes(token) || index.landmark.includes(token))) {
      tokenScore = 50;
    }
    if (!tokenScore && index.item.includes(token)) tokenScore = 20;

    if (tokenScore === 0) return 0;
    score += tokenScore;
  }

  return score;
}

export function distanceTo(
  delivery: Pick<Delivery, 'latitude' | 'longitude'>,
  position: LatLng | null,
): number | null {
  if (!position || delivery.latitude === null || delivery.longitude === null) return null;
  return haversineMeters(position, { lat: delivery.latitude, lng: delivery.longitude });
}

export interface FilterOptions {
  query: string;
  status: StatusFilter;
  sort: SortMode;
  position: LatLng | null;
}

export function applyFilters(deliveries: Delivery[], options: FilterOptions): Delivery[] {
  const { query, status, sort, position } = options;
  const trimmed = query.trim();

  const filtered = deliveries.filter((delivery) => {
    if (status !== 'all' && delivery.status !== status) return false;
    if (!trimmed) return true;
    return scoreMatch(buildSearchIndex(delivery), trimmed) > 0;
  });

  return filtered.sort((a, b) => {
    if (sort === 'nearest') {
      const da = distanceTo(a, position);
      const db = distanceTo(b, position);
      if (da === null && db === null) {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
      if (da === null) return 1;
      if (db === null) return -1;
      if (Math.abs(da - db) > 5) return da - db;
    }
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  });
}

export function matchesQuery(delivery: Delivery, query: string): boolean {
  if (!query.trim()) return true;
  return scoreMatch(buildSearchIndex(delivery), query) > 0;
}
