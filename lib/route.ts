import { haversineKm, ROAD_FACTOR, CITY_SPEED_KMH } from './geo';
import type { LatLng } from './types';

export interface RouteResult {
  /** Leaflet-friendly [lat, lng] pairs */
  coordinates: Array<[number, number]>;
  distanceKm: number;
  durationMin: number;
  source: 'osrm' | 'straight';
}

export function straightRoute(from: LatLng, to: LatLng): RouteResult {
  const distanceKm = haversineKm(from, to) * ROAD_FACTOR;
  return {
    coordinates: [
      [from.lat, from.lng],
      [to.lat, to.lng],
    ],
    distanceKm,
    durationMin: Math.max(1, Math.round((distanceKm / CITY_SPEED_KMH) * 60)),
    source: 'straight',
  };
}

/**
 * Free road routing through the public OSRM demo server (no API key).
 * Falls back to a straight line when offline or rate limited.
 */
export async function fetchRoute(from: LatLng, to: LatLng): Promise<RouteResult> {
  const url =
    `https://router.project-osrm.org/route/v1/driving/` +
    `${from.lng},${from.lat};${to.lng},${to.lat}` +
    `?overview=full&geometries=geojson&steps=false`;

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 6500);

  try {
    const response = await fetch(url, { signal: controller.signal, cache: 'no-store' });
    if (!response.ok) throw new Error(`osrm ${response.status}`);

    const payload = (await response.json()) as {
      routes?: Array<{
        distance?: number;
        duration?: number;
        geometry?: { coordinates?: Array<[number, number]> };
      }>;
    };

    const route = payload.routes?.[0];
    const coordinates = route?.geometry?.coordinates;
    if (!route || !coordinates || coordinates.length < 2) throw new Error('empty route');

    return {
      coordinates: coordinates.map(([lng, lat]) => [lat, lng] as [number, number]),
      distanceKm: (route.distance ?? 0) / 1000,
      durationMin: Math.max(1, Math.round((route.duration ?? 0) / 60)),
      source: 'osrm',
    };
  } catch {
    return straightRoute(from, to);
  } finally {
    clearTimeout(timeout);
  }
}

export interface PlaceResult {
  label: string;
  lat: number;
  lng: number;
}

/** Free place search through Nominatim — used inside the map picker. */
export async function searchPlaces(query: string): Promise<PlaceResult[]> {
  const trimmed = query.trim();
  if (trimmed.length < 3) return [];

  const params = new URLSearchParams({
    format: 'jsonv2',
    q: trimmed,
    limit: '5',
    'accept-language': 'ckb,ar,en',
  });

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 6500);

  try {
    const response = await fetch(`https://nominatim.openstreetmap.org/search?${params.toString()}`, {
      signal: controller.signal,
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    });
    if (!response.ok) return [];
    const payload = (await response.json()) as Array<{
      display_name?: string;
      lat?: string;
      lon?: string;
    }>;

    return payload
      .map((item) => ({
        label: item.display_name ?? '',
        lat: Number(item.lat),
        lng: Number(item.lon),
      }))
      .filter((item) => item.label && Number.isFinite(item.lat) && Number.isFinite(item.lng))
      .slice(0, 5);
  } catch {
    return [];
  } finally {
    clearTimeout(timeout);
  }
}
