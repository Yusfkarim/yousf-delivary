'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Car,
  Copy,
  Crosshair,
  ExternalLink,
  Loader2,
  MapPin,
  Navigation,
  Route,
  RotateCw,
  Timer,
} from 'lucide-react';
import { Sheet } from '@/components/ui/Sheet';
import { MapView } from '@/components/map/MapView';
import { useStore } from '@/lib/store';
import { useToast } from '@/components/ui/Toast';
import { t, statusMeta } from '@/lib/i18n';
import type { Delivery, LatLng } from '@/lib/types';
import { copyText } from '@/lib/clipboard';
import { fetchRoute, straightRoute, type RouteResult } from '@/lib/route';
import { haversineMeters } from '@/lib/geo';
import { formatDistanceDistance, formatDuration, formatMoney } from '@/lib/format';
import { appleMapsUrl, googleDirectionsUrl, googlePinUrl, wazeUrl } from '@/lib/geo';
import { formatPhone } from '@/lib/phone';

interface RouteModalProps {
  delivery: Delivery | null;
  onClose: () => void;
}

export function RouteModal({ delivery, onClose }: RouteModalProps) {
  const { position, settings, requestPosition, locationState } = useStore();
  const { toast } = useToast();
  const [route, setRoute] = useState<RouteResult | null>(null);
  const [loading, setLoading] = useState(false);
  const kurdish = settings.kurdishDigits;

  const customer = useMemo<LatLng | null>(() => {
    if (!delivery || delivery.latitude === null || delivery.longitude === null) return null;
    return { lat: delivery.latitude, lng: delivery.longitude };
  }, [delivery]);

  const loadRoute = useCallback(async () => {
    if (!customer) return;
    if (!position) {
      setRoute(null);
      return;
    }
    setLoading(true);
    const result = await fetchRoute(position, customer);
    setRoute(result);
    setLoading(false);
  }, [customer, position]);

  // Re-route only when the order changes or the driver moved ~110m.
  const routeKeyRef = useRef('');
  const routeKey = delivery
    ? `${delivery.id}:${position ? `${position.lat.toFixed(3)},${position.lng.toFixed(3)}` : 'none'}`
    : '';

  useEffect(() => {
    if (!delivery || !customer) return;
    if (routeKeyRef.current === routeKey) return;
    routeKeyRef.current = routeKey;
    void loadRoute();
  }, [routeKey, delivery, customer, loadRoute]);

  if (!delivery) return null;

  const straightMeters =
    position && customer ? haversineMeters(position, customer) : null;
  const displayMeters = route ? Math.round(route.distanceKm * 1000) : straightMeters;
  const fallbackRoute = position && customer ? straightRoute(position, customer) : null;
  const polyline = route?.coordinates ?? fallbackRoute?.coordinates ?? null;
  const etaMinutes = route?.durationMin ?? fallbackRoute?.durationMin ?? null;

  const copy = async (value: string, successMessage: string) => {
    const ok = await copyText(value);
    toast(ok ? successMessage : t.copyFailed, ok ? 'success' : 'error');
  };

  const addressLine =
    [delivery.address, delivery.landmark].filter(Boolean).join(' — ') || t.noAddress;
  const navigationHref = customer ? googleDirectionsUrl(position, customer) : null;

  return (
    <Sheet
      open={Boolean(delivery)}
      onClose={onClose}
      variant="full"
      title={t.mapModalTitle}
      subtitle={`${delivery.name} · ${formatPhone(delivery.primaryPhone)}`}
      footer={
        <div className="space-y-2">
          {navigationHref ? (
            <a
              href={navigationHref}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary w-full"
            >
              <Navigation className="h-4 w-4" aria-hidden />
              {t.startNavigation}
            </a>
          ) : (
            <button type="button" disabled className="btn-primary w-full">
              <Navigation className="h-4 w-4" aria-hidden />
              {t.startNavigation}
            </button>
          )}
          <div className="grid grid-cols-3 gap-1.5">
            {customer ? (
              <a
                href={wazeUrl(customer)}
                target="_blank"
                rel="noopener noreferrer"
                className="btn-ghost px-2 py-2 text-[11px]"
              >
                <Car className="h-3.5 w-3.5" aria-hidden />
                Waze
              </a>
            ) : (
              <span className="btn-ghost cursor-not-allowed px-2 py-2 text-[11px] opacity-40">
                <Car className="h-3.5 w-3.5" aria-hidden />
                Waze
              </span>
            )}
            {customer ? (
              <a
                href={appleMapsUrl(customer)}
                target="_blank"
                rel="noopener noreferrer"
                className="btn-ghost px-2 py-2 text-[11px]"
              >
                <MapPin className="h-3.5 w-3.5" aria-hidden />
                Apple
              </a>
            ) : (
              <span className="btn-ghost cursor-not-allowed px-2 py-2 text-[11px] opacity-40">
                <MapPin className="h-3.5 w-3.5" aria-hidden />
                Apple
              </span>
            )}
            <button
              type="button"
              onClick={() =>
                void copy(
                  customer ? `${customer.lat.toFixed(6)}, ${customer.lng.toFixed(6)}` : '',
                  t.copyCoords,
                )
              }
              className="btn-ghost px-2 py-2 text-[11px]"
            >
              <Copy className="h-3.5 w-3.5" aria-hidden />
              {t.copyCoords}
            </button>
          </div>
        </div>
      }
    >
      <div className="space-y-3">
        <div className="relative h-[38dvh] min-h-[220px] overflow-hidden rounded-2xl border border-white/10">
          <MapView
            markers={[
              {
                id: delivery.id,
                position: customer ?? { lat: 35.5556, lng: 45.4351 },
                label: delivery.name,
                kind: 'customer',
                status: delivery.status,
              },
            ]}
            me={position}
            route={polyline}
            routeDash={!route}
            fitToMarkers={Boolean(customer)}
            showZoomControls={false}
          />

          <div className="pointer-events-none absolute inset-x-0 top-0 flex items-start justify-between gap-2 p-2">
            <span className="pointer-events-auto chip border-white/15 bg-ink-950/80 backdrop-blur-md">
              <span className="h-1.5 w-1.5 rounded-full bg-gold" />
              {t.customerLocation}
            </span>
            <span className="pointer-events-auto chip border-white/15 bg-ink-950/80 backdrop-blur-md">
              <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-neon" />
              {t.myLocation}
            </span>
          </div>

          {loading && (
            <div className="absolute inset-x-0 bottom-0 flex items-center justify-center gap-2 bg-ink-950/70 py-1.5 text-[10.5px] text-neon-soft backdrop-blur">
              <Loader2 className="h-3 w-3 animate-spin" aria-hidden />
              {t.refreshRoute}
            </div>
          )}
        </div>

        {!customer && (
          <div className="rounded-2xl border border-gold/30 bg-gold/10 p-3 text-[12px] leading-6 text-gold-soft">
            {t.noLocationForDistance}
          </div>
        )}

        {customer && !position && (
          <button
            type="button"
            onClick={requestPosition}
            className="flex w-full items-center justify-center gap-2 rounded-2xl border border-gold/30 bg-gold/10 px-3 py-2.5 text-[12px] font-semibold text-gold-soft"
          >
            <Crosshair className="h-4 w-4" aria-hidden />
            {locationState === 'locating' ? t.locating : t.enableLocation}
          </button>
        )}

        <div className="grid grid-cols-2 gap-2">
          <div className="glass-soft flex items-center gap-2.5 p-3">
            <span className="grid h-9 w-9 place-items-center rounded-xl border border-neon/25 bg-neon/10 text-neon-soft">
              <Route className="h-4 w-4" aria-hidden />
            </span>
            <span className="min-w-0">
              <span className="block text-[10px] text-slate-400">{t.distanceLabel}</span>
              <span className="block truncate text-[14px] font-bold text-white">
                {displayMeters !== null ? formatDistanceDistance(displayMeters, kurdish) : '—'}
              </span>
            </span>
          </div>
          <div className="glass-soft flex items-center gap-2.5 p-3">
            <span className="grid h-9 w-9 place-items-center rounded-xl border border-gold/25 bg-gold/10 text-gold-soft">
              <Timer className="h-4 w-4" aria-hidden />
            </span>
            <span className="min-w-0">
              <span className="block text-[10px] text-slate-400">{t.etaLabel}</span>
              <span className="block truncate text-[14px] font-bold text-white">
                {etaMinutes !== null ? formatDuration(etaMinutes, kurdish) : '—'}
              </span>
            </span>
          </div>
        </div>

        <div className="glass-soft space-y-2 p-3">
          <div className="flex items-start gap-2">
            <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-neon-soft" aria-hidden />
            <div className="min-w-0 flex-1">
              <p className="text-[10px] text-slate-400">{t.customerLocation}</p>
              <p className="text-[12.5px] leading-6 text-slate-100">{addressLine}</p>
              {customer && (
                <button
                  type="button"
                  onClick={() => void copy(`${customer.lat.toFixed(6)}, ${customer.lng.toFixed(6)}`, t.copyCoords)}
                  dir="ltr"
                  className="mt-1 inline-flex items-center gap-1 rounded-lg border border-white/10 bg-white/[0.04] px-2 py-1 text-[10px] text-slate-300"
                >
                  <Copy className="h-3 w-3" aria-hidden />
                  {customer.lat.toFixed(5)}, {customer.lng.toFixed(5)}
                </button>
              )}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-1.5 border-t border-white/[0.06] pt-2">
            <span className="chip chip-gold">
              {formatMoney(delivery.price + delivery.deliveryFee, delivery.currency, { kurdish })}
            </span>
            <span className={`chip ${statusMeta[delivery.status].chip}`}>
              {statusMeta[delivery.status].label}
            </span>
            <span className="chip">{t.roadRoute}</span>
            <div className="mr-auto flex items-center gap-1">
              <button
                type="button"
                onClick={() => void copy(addressLine, t.copied)}
                className="icon-btn-sm h-7 w-7"
                aria-label={t.copyAddress}
                title={t.copyAddress}
              >
                <Copy className="h-3 w-3" aria-hidden />
              </button>
              {customer && (
                <a
                  href={googlePinUrl(customer.lat, customer.lng)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="icon-btn-sm h-7 w-7"
                  aria-label={t.map}
                  title={t.map}
                >
                  <ExternalLink className="h-3 w-3" aria-hidden />
                </a>
              )}
              <button
                type="button"
                onClick={() => void loadRoute()}
                className="icon-btn-sm h-7 w-7"
                aria-label={t.refreshRoute}
                title={t.refreshRoute}
              >
                <RotateCw className="h-3 w-3" aria-hidden />
              </button>
            </div>
          </div>
        </div>

        {route?.source === 'straight' && position && (
          <p className="px-1 text-[10.5px] leading-5 text-slate-500">{t.routeUnavailable}</p>
        )}
      </div>
    </Sheet>
  );
}
