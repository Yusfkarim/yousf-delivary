'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import {
  ChevronLeft,
  Crosshair,
  Layers,
  MessageCircle,
  Navigation,
  Phone,
  X,
} from 'lucide-react';
import clsx from 'clsx';
import { MapView } from '@/components/map/MapView';
import { useStore } from '@/lib/store';
import { t, statusMeta } from '@/lib/i18n';
import { distanceTo } from '@/lib/search';
import { formatDistanceDistance, formatMoney } from '@/lib/format';
import { googleDirectionsUrl } from '@/lib/geo';
import { toTelHref, toWhatsAppHref } from '@/lib/phone';
import { StatusBadge } from '@/components/StatusBadge';
import type { StatusFilter } from '@/lib/types';

const FILTERS: Array<{ value: StatusFilter; label: string }> = [
  { value: 'all', label: t.filterAll },
  { value: 'pending', label: t.filterPending },
  { value: 'on_the_way', label: t.filterOnTheWay },
  { value: 'delivered', label: t.filterDelivered },
];

export default function MapPage() {
  const { deliveries, position, requestPosition, settings } = useStore();
  const [status, setStatus] = useState<StatusFilter>('all');
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const kurdish = settings.kurdishDigits;

  const filtered = useMemo(
    () => deliveries.filter((item) => (status === 'all' ? true : item.status === status)),
    [deliveries, status],
  );

  const markers = useMemo(
    () =>
      filtered
        .filter((item) => item.latitude !== null && item.longitude !== null)
        .map((item) => ({
          id: item.id,
          position: { lat: item.latitude as number, lng: item.longitude as number },
          label: item.name,
          kind: 'customer' as const,
          status: item.status,
          active: item.id === selectedId,
        })),
    [filtered, selectedId],
  );

  const selected = deliveries.find((item) => item.id === selectedId) ?? null;
  const selectedMeters = selected ? distanceTo(selected, position) : null;

  return (
    <div className="space-y-3 pt-1">
      <div className="flex items-center gap-2">
        <h1 className="text-[15px] font-bold text-white">{t.navMap}</h1>
        <span className="chip chip-neon text-[10px]">
          <Layers className="h-3 w-3" aria-hidden />
          {markers.length} {t.pins}
        </span>
        <Link href="/" className="mr-auto text-[11px] text-slate-400">
          {t.navHome}
        </Link>
      </div>

      <div className="no-scrollbar -mx-1 flex gap-1.5 overflow-x-auto px-1">
        {FILTERS.map((filter) => (
          <button
            key={filter.value}
            type="button"
            onClick={() => setStatus(filter.value)}
            aria-pressed={status === filter.value}
            className={clsx('pill py-1.5 text-[11.5px]', status === filter.value ? 'pill-active' : 'pill-idle')}
          >
            {filter.label}
            <span className="rounded-full bg-white/10 px-1.5 text-[9.5px]">
              {filter.value === 'all'
                ? deliveries.length
                : deliveries.filter((item) => item.status === filter.value).length}
            </span>
          </button>
        ))}
      </div>

      <div className="relative h-[calc(100dvh-var(--nav-height)-13rem)] min-h-[320px] overflow-hidden rounded-3xl border border-white/10">
        <MapView
          markers={markers}
          me={position}
          onMarkerClick={(id) => setSelectedId(id)}
          fitToMarkers={markers.length > 0}
          className="h-full w-full"
        />

        <button
          type="button"
          onClick={requestPosition}
          aria-label={t.myLocation}
          className="absolute bottom-3 left-3 grid h-11 w-11 place-items-center rounded-2xl border border-neon/35 bg-ink-950/85 text-neon-soft shadow-neon backdrop-blur-md active:scale-95"
        >
          <Crosshair className="h-5 w-5" aria-hidden />
        </button>

        <div className="pointer-events-none absolute right-3 top-3 space-y-1 rounded-xl border border-white/10 bg-ink-950/80 p-2 text-[9.5px] text-slate-300 backdrop-blur-md">
          {(['pending', 'on_the_way', 'delivered'] as const).map((key) => (
            <div key={key} className="flex items-center gap-1.5">
              <span className={clsx('h-1.5 w-1.5 rounded-full', statusMeta[key].dot)} />
              {statusMeta[key].shortLabel}
            </div>
          ))}
        </div>

        {markers.length === 0 && (
          <div className="pointer-events-none absolute inset-0 grid place-items-center bg-ink-950/40">
            <p className="rounded-2xl border border-white/10 bg-ink-950/85 px-4 py-2.5 text-[11.5px] text-slate-300 backdrop-blur">
              {t.noLocationForDistance}
            </p>
          </div>
        )}
      </div>

      {selected && (
        <div className="glass animate-fade-up p-3.5">
          <div className="flex items-start gap-2">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-1.5">
                <h2 className="truncate text-[14px] font-bold text-white">{selected.name}</h2>
                <StatusBadge status={selected.status} short />
              </div>
              <p className="mt-0.5 truncate text-[11.5px] text-slate-400">
                {selected.address || selected.landmark || t.noAddress}
              </p>
              <p className="mt-0.5 flex items-center gap-2 text-[10.5px]">
                {selectedMeters !== null && (
                  <span className="text-neon-soft">{formatDistanceDistance(selectedMeters, kurdish)}</span>
                )}
                <span className="text-gold-soft">
                  {formatMoney(selected.price + selected.deliveryFee, selected.currency, { kurdish })}
                </span>
              </p>
            </div>
            <button
              type="button"
              onClick={() => setSelectedId(null)}
              className="icon-btn-sm h-8 w-8"
              aria-label={t.close}
            >
              <X className="h-3.5 w-3.5" aria-hidden />
            </button>
          </div>

          <div className="mt-2.5 grid grid-cols-4 gap-1.5">
            <a href={toTelHref(selected.primaryPhone)} className="flex flex-col items-center gap-1 rounded-xl border border-neon/25 bg-neon/10 py-2 text-[10px] font-semibold text-neon-soft">
              <Phone className="h-4 w-4" aria-hidden />
              {t.call}
            </a>
            <a
              href={toWhatsAppHref(selected.primaryPhone, t.etaTemplate(selected.name))}
              target="_blank"
              rel="noopener noreferrer"
              className="flex flex-col items-center gap-1 rounded-xl border border-[#25D366]/25 bg-[#25D366]/10 py-2 text-[10px] font-semibold text-[#3ee27a]"
            >
              <MessageCircle className="h-4 w-4" aria-hidden />
              {t.whatsapp}
            </a>
            <a
              href={googleDirectionsUrl(
                position,
                selected.latitude !== null && selected.longitude !== null
                  ? { lat: selected.latitude, lng: selected.longitude }
                  : { lat: 0, lng: 0 },
              )}
              target="_blank"
              rel="noopener noreferrer"
              className="flex flex-col items-center gap-1 rounded-xl border border-gold/25 bg-gold/10 py-2 text-[10px] font-semibold text-gold-soft"
            >
              <Navigation className="h-4 w-4" aria-hidden />
              {t.navigate}
            </a>
            <Link
              href={`/orders/${selected.id}`}
              className="flex flex-col items-center gap-1 rounded-xl border border-white/10 bg-white/[0.05] py-2 text-[10px] font-semibold text-slate-200"
            >
              <ChevronLeft className="h-4 w-4" aria-hidden />
              {t.viewDetails}
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
