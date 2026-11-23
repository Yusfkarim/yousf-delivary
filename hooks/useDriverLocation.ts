'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { LatLng } from '@/lib/types';
import { readCachedLocation, writeCachedLocation } from '@/lib/cache';

export type LocationState = 'idle' | 'locating' | 'ready' | 'denied' | 'unsupported' | 'error';

export interface DriverLocation {
  position: LatLng | null;
  accuracy: number | null;
  state: LocationState;
  updatedAt: string | null;
  request: () => void;
  watching: boolean;
}

/**
 * Live driver position: hydrates instantly from the last known cached fix,
 * then follows `navigator.geolocation.watchPosition`.
 */
export function useDriverLocation(): DriverLocation {
  const [position, setPosition] = useState<LatLng | null>(null);
  const [accuracy, setAccuracy] = useState<number | null>(null);
  const [state, setState] = useState<LocationState>('idle');
  const [updatedAt, setUpdatedAt] = useState<string | null>(null);
  const [watching, setWatching] = useState(false);
  const watchId = useRef<number | null>(null);
  const mounted = useRef(true);

  const applyPosition = useCallback((result: GeolocationPosition) => {
    if (!mounted.current) return;
    const coords = result.coords;
    const next = { lat: coords.latitude, lng: coords.longitude };
    setPosition(next);
    setAccuracy(Number.isFinite(coords.accuracy) ? coords.accuracy : null);
    setState('ready');
    setUpdatedAt(new Date().toISOString());
    writeCachedLocation(next.lat, next.lng);
  }, []);

  const handleError = useCallback((error: GeolocationPositionError) => {
    if (!mounted.current) return;
    if (error.code === error.PERMISSION_DENIED) setState('denied');
    else setState((prev) => (prev === 'ready' ? 'ready' : 'error'));
  }, []);

  const startWatch = useCallback(() => {
    if (typeof window === 'undefined' || !('geolocation' in navigator)) {
      setState('unsupported');
      return;
    }
    if (watchId.current !== null) return;
    setWatching(true);
    watchId.current = navigator.geolocation.watchPosition(applyPosition, handleError, {
      enableHighAccuracy: true,
      maximumAge: 5_000,
      timeout: 20_000,
    });
  }, [applyPosition, handleError]);

  const request = useCallback(() => {
    if (typeof window === 'undefined' || !('geolocation' in navigator)) {
      setState('unsupported');
      return;
    }
    setState((prev) => (prev === 'ready' ? prev : 'locating'));
    navigator.geolocation.getCurrentPosition(
      (result) => {
        applyPosition(result);
        startWatch();
      },
      handleError,
      { enableHighAccuracy: true, maximumAge: 0, timeout: 20_000 },
    );
  }, [applyPosition, handleError, startWatch]);

  useEffect(() => {
    mounted.current = true;
    const cached = readCachedLocation();
    if (cached) {
      setPosition({ lat: cached.lat, lng: cached.lng });
      setUpdatedAt(cached.at);
      setState('ready');
    }

    if (typeof window === 'undefined' || !('geolocation' in navigator)) {
      setState('unsupported');
      return () => {
        mounted.current = false;
      };
    }

    // Ask once on mount; browsers show the permission sheet.
    setState((prev) => (prev === 'ready' ? prev : 'locating'));
    navigator.geolocation.getCurrentPosition(
      (result) => {
        applyPosition(result);
        startWatch();
      },
      handleError,
      { enableHighAccuracy: true, maximumAge: 30_000, timeout: 20_000 },
    );

    return () => {
      mounted.current = false;
      if (watchId.current !== null) {
        navigator.geolocation.clearWatch(watchId.current);
        watchId.current = null;
      }
    };
  }, [applyPosition, handleError, startWatch]);

  return { position, accuracy, state, updatedAt, request, watching };
}
