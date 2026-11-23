import type { LatLng } from './types';

const EARTH_RADIUS_KM = 6371.0088;
/** Average city-driving speed inside Sulaymaniyah / Erbil (km/h). */
export const CITY_SPEED_KMH = 26;
/** Roads are never a straight line — factor used for ETA only. */
export const ROAD_FACTOR = 1.32;

export function toRad(value: number): number {
  return (value * Math.PI) / 180;
}

/** Great-circle distance in kilometres (Haversine formula). */
export function haversineKm(a: LatLng, b: LatLng): number {
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);

  const h =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);

  return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(h)));
}

/** Distance in metres (rounded). */
export function haversineMeters(a: LatLng, b: LatLng): number {
  return Math.round(haversineKm(a, b) * 1000);
}

export function isValidLat(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= -90 && value <= 90;
}

export function isValidLng(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= -180 && value <= 180;
}

export function isValidLatLng(value: unknown): value is LatLng {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as Partial<LatLng>;
  return isValidLat(candidate.lat) && isValidLng(candidate.lng);
}

export function roundCoord(value: number): number {
  return Math.round(value * 1e6) / 1e6;
}

/** ETA minutes between two points, accounting for the road factor. */
export function etaMinutes(distanceKm: number, speedKmh = CITY_SPEED_KMH): number {
  if (!Number.isFinite(distanceKm) || distanceKm <= 0) return 0;
  const minutes = (distanceKm * ROAD_FACTOR * 60) / speedKmh;
  return Math.max(1, Math.round(minutes));
}

/** Bounding box used to fit both markers on a map. */
export function boundsOf(points: LatLng[], padding = 0.004): [[number, number], [number, number]] {
  const lats = points.map((p) => p.lat);
  const lngs = points.map((p) => p.lng);
  return [
    [Math.min(...lats) - padding, Math.min(...lngs) - padding],
    [Math.max(...lats) + padding, Math.max(...lngs) + padding],
  ];
}

/** Sulaymaniyah city centre — used as a sane default map view. */
export const SULAYMANIYAH: LatLng = { lat: 35.5556, lng: 45.4351 };

export function coordsToString(lat: number, lng: number): string {
  return `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
}

/**
 * Extract coordinates from a pasted Google Maps / Apple Maps / OSM link,
 * or from a plain "35.5556, 45.4351" string.
 *
 * Supported shapes:
 *   https://maps.google.com/?q=35.55,45.43
 *   https://www.google.com/maps/@35.55,45.43,17z
 *   https://www.google.com/maps/place/Name/@35.55,45.43,17z/data=!3d35.55!4d45.43
 *   https://maps.apple.com/?ll=35.55,45.43
 *   https://www.openstreetmap.org/#map=17/35.55/45.43
 *   geo:35.55,45.43
 *   35.55, 45.43
 */
export function parseCoordsFromText(input: string): LatLng | null {
  if (!input) return null;
  const text = input.trim();

  const patterns: RegExp[] = [
    /[?&](?:q|query|ll|center|destination|daddr)=(-?\d{1,3}(?:\.\d+)?)\s*[,%2C]+\s*(-?\d{1,3}(?:\.\d+)?)/i,
    /!3d(-?\d{1,3}(?:\.\d+)?)!4d(-?\d{1,3}(?:\.\d+)?)/i,
    /@(-?\d{1,3}(?:\.\d+)?),(-?\d{1,3}(?:\.\d+)?)/,
    /#map=\d+(?:\.\d+)?\/(-?\d{1,3}(?:\.\d+)?)\/(-?\d{1,3}(?:\.\d+)?)/,
    /geo:(-?\d{1,3}(?:\.\d+)?),(-?\d{1,3}(?:\.\d+)?)/i,
    /^\s*(-?\d{1,3}(?:\.\d+)?)\s*[,;\s]\s*(-?\d{1,3}(?:\.\d+)?)\s*$/,
  ];

  for (const pattern of patterns) {
    const match = text.match(pattern);
    if (!match) continue;
    const lat = Number(match[1]);
    const lng = Number(match[2]);
    if (isValidLat(lat) && isValidLng(lng)) return { lat, lng };
  }

  return null;
}

export function googleDirectionsUrl(origin: LatLng | null, destination: LatLng): string {
  // Keep the comma literal: Google Maps, iOS and Android intents all accept it
  // and it survives round-tripping better than %2C.
  const parts = [
    'api=1',
    `destination=${destination.lat},${destination.lng}`,
    'travelmode=driving',
    'dir_action=navigate',
  ];
  if (origin) parts.push(`origin=${origin.lat},${origin.lng}`);
  return `https://www.google.com/maps/dir/?${parts.join('&')}`;
}

export function googlePinUrl(lat: number, lng: number): string {
  return `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
}

export function wazeUrl(destination: LatLng): string {
  return `https://waze.com/ul?ll=${destination.lat},${destination.lng}&navigate=yes`;
}

export function appleMapsUrl(destination: LatLng): string {
  return `https://maps.apple.com/?daddr=${destination.lat},${destination.lng}&dirflg=d`;
}
