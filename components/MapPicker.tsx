'use client';

import { useCallback, useState } from 'react';
import { Crosshair, Link2, Loader2, MapPin, Search, Trash2, X } from 'lucide-react';
import { MapView } from '@/components/map/MapView';
import { useStore } from '@/lib/store';
import { useToast } from '@/components/ui/Toast';
import { parseCoordsFromText, coordsToString } from '@/lib/geo';
import { searchPlaces, type PlaceResult } from '@/lib/route';
import { t } from '@/lib/i18n';
import type { LatLng } from '@/lib/types';

interface MapPickerProps {
  value: LatLng | null;
  onChange: (value: LatLng | null) => void;
  error?: string;
}

export function MapPicker({ value, onChange, error }: MapPickerProps) {
  const { position, requestPosition } = useStore();
  const { toast } = useToast();
  const [link, setLink] = useState('');
  const [locating, setLocating] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<PlaceResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [showSearch, setShowSearch] = useState(false);

  const captureGps = useCallback(() => {
    if (!('geolocation' in navigator)) {
      toast(t.locationUnsupported, 'error');
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (result) => {
        setLocating(false);
        onChange({ lat: result.coords.latitude, lng: result.coords.longitude });
        toast(t.gpsCaptured, 'success');
      },
      () => {
        setLocating(false);
        toast(t.gpsError, 'error');
      },
      { enableHighAccuracy: true, maximumAge: 0, timeout: 20_000 },
    );
  }, [onChange, toast]);

  const extractLink = () => {
    const coords = parseCoordsFromText(link);
    if (coords) {
      onChange(coords);
      setLink('');
      toast(t.coordsExtracted, 'success');
    } else if (/goo\.gl|maps\.app/i.test(link)) {
      toast(t.shortLinkWarning, 'error');
    } else {
      toast(t.coordsInvalid, 'error');
    }
  };

  const runSearch = async () => {
    if (query.trim().length < 3) return;
    setSearching(true);
    const found = await searchPlaces(query);
    setResults(found);
    setSearching(false);
    if (found.length === 0) toast(t.searchEmptyTitle, 'info');
  };

  return (
    <div className="space-y-2">
      <div className="relative h-64 overflow-hidden rounded-2xl border border-white/10">
        <MapView
          markers={
            value
              ? [
                  {
                    id: 'picked',
                    position: value,
                    label: t.customerLocation,
                    kind: 'customer',
                    active: true,
                  },
                ]
              : []
          }
          me={position}
          center={value ?? position ?? undefined}
          zoom={value ? 16 : 14}
          onMapClick={(next) => onChange(next)}
          panTo={value}
          fitToMarkers={false}
          showZoomControls
        />

        <div className="pointer-events-none absolute inset-x-0 top-0 flex items-center justify-between gap-2 p-2">
          <span className="chip max-w-[58%] truncate border-white/15 bg-ink-950/80 backdrop-blur-md">
            <MapPin className="h-3 w-3 shrink-0 text-neon-soft" aria-hidden />
            <span className="truncate" dir={value ? 'ltr' : 'rtl'}>
              {value ? coordsToString(value.lat, value.lng) : 'GPS'}
            </span>
          </span>
          <div className="pointer-events-auto flex gap-1">
            <button
              type="button"
              onClick={captureGps}
              aria-label={t.captureGps}
              title={t.captureGps}
              className="grid h-9 w-9 place-items-center rounded-xl border border-neon/35 bg-ink-950/85 text-neon-soft backdrop-blur-md active:scale-95"
            >
              {locating ? (
                <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
              ) : (
                <Crosshair className="h-4 w-4" aria-hidden />
              )}
            </button>
            <button
              type="button"
              onClick={() => {
                requestPosition();
                if (position) onChange(position);
              }}
              aria-label={t.myLocation}
              title={t.myLocation}
              className="grid h-9 w-9 place-items-center rounded-xl border border-white/15 bg-ink-950/85 text-slate-200 backdrop-blur-md active:scale-95"
            >
              <MapPin className="h-4 w-4" aria-hidden />
            </button>
            {value && (
              <button
                type="button"
                onClick={() => onChange(null)}
                aria-label={t.clearLocation}
                title={t.clearLocation}
                className="grid h-9 w-9 place-items-center rounded-xl border border-danger/35 bg-ink-950/85 text-danger backdrop-blur-md active:scale-95"
              >
                <Trash2 className="h-4 w-4" aria-hidden />
              </button>
            )}
          </div>
        </div>
      </div>

      <p className="px-1 text-[10.5px] leading-5 text-slate-500">{t.mapPickerHint}</p>

      <button
        type="button"
        onClick={captureGps}
        disabled={locating}
        className="btn-ghost w-full py-2.5 text-[12.5px]"
      >
        {locating ? (
          <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
        ) : (
          <Crosshair className="h-4 w-4 text-neon-soft" aria-hidden />
        )}
        {locating ? t.capturingGps : t.captureGps}
    </button>

      <div className="flex gap-1.5">
        <div className="relative flex-1">
          <Link2 className="pointer-events-none absolute right-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-500" aria-hidden />
          <input
            value={link}
            onChange={(event) => setLink(event.target.value)}
            placeholder={t.pasteMapsLinkPlaceholder}
            dir="ltr"
            className="w-full rounded-xl border border-white/10 bg-ink-950/60 py-2.5 pr-9 pl-3 text-left text-[12px] text-slate-100 placeholder:text-right placeholder:text-slate-500 outline-none focus:border-neon/50"
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault();
                extractLink();
              }
            }}
          />
        </div>
        <button type="button" onClick={extractLink} className="btn-ghost shrink-0 px-3 py-2.5 text-[11.5px]">
          {t.extractCoords}
        </button>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => setShowSearch((value) => !value)}
          className="inline-flex items-center gap-1.5 text-[11px] text-slate-400 underline decoration-dotted"
        >
          <Search className="h-3 w-3" aria-hidden />
          {t.searchPlaceholder.split('،')[0]}
        </button>
        {value && (
          <span className="chip chip-neon text-[10px]" dir="ltr">
            {value.lat.toFixed(5)}, {value.lng.toFixed(5)}
          </span>
        )}
      </div>

      {showSearch && (
        <div className="glass-inset space-y-2 p-2 animate-fade-up">
          <div className="flex gap-1.5">
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault();
                  void runSearch();
                }
              }}
              placeholder="گەڕان بۆ شوێن (نەخشەی OpenStreetMap)"
              className="flex-1 rounded-xl border border-white/10 bg-ink-950/60 px-3 py-2 text-[12px] text-slate-100 placeholder:text-slate-500 outline-none focus:border-neon/50"
            />
            <button
              type="button"
              onClick={() => void runSearch()}
              className="grid h-9 w-9 place-items-center rounded-xl border border-neon/30 bg-neon/10 text-neon-soft"
              aria-label={t.searchPlaceholder}
            >
              {searching ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden /> : <Search className="h-4 w-4" aria-hidden />}
            </button>
          </div>

          {results.length > 0 && (
            <ul className="max-h-40 space-y-1 overflow-y-auto">
              {results.map((result) => (
                <li key={`${result.lat}-${result.lng}`}>
                  <button
                    type="button"
                    onClick={() => {
                      onChange({ lat: result.lat, lng: result.lng });
                      setResults([]);
                      setShowSearch(false);
                      setQuery('');
                    }}
                    className="flex w-full items-start gap-2 rounded-lg p-1.5 text-right text-[11px] leading-5 text-slate-300 hover:bg-white/5"
                  >
                    <MapPin className="mt-1 h-3 w-3 shrink-0 text-neon-soft" aria-hidden />
                    <span className="line-clamp-2 flex-1">{result.label}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}

          {results.length === 0 && !searching && (
            <button
              type="button"
              onClick={() => {
                setShowSearch(false);
                setResults([]);
              }}
              className="flex w-full items-center justify-center gap-1 text-[10.5px] text-slate-500"
            >
              <X className="h-3 w-3" aria-hidden />
              {t.close}
            </button>
          )}
        </div>
      )}

      {error && <p className="px-1 text-[11px] text-danger">{error}</p>}
    </div>
  );
}
