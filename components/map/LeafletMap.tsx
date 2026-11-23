'use client';

import { useEffect, useMemo } from 'react';
import L from 'leaflet';
import {
  MapContainer,
  Marker,
  Polyline,
  TileLayer,
  Tooltip,
  useMap,
  useMapEvents,
} from 'react-leaflet';
import type { DeliveryStatus, LatLng } from '@/lib/types';
import { t } from '@/lib/i18n';

export interface MapMarker {
  id: string;
  position: LatLng;
  label?: string;
  kind: 'customer' | 'me' | 'plain';
  status?: DeliveryStatus;
  active?: boolean;
}

export interface LeafletMapProps {
  center?: LatLng;
  zoom?: number;
  markers?: MapMarker[];
  me?: LatLng | null;
  route?: Array<[number, number]> | null;
  routeDash?: boolean;
  onMapClick?: (position: LatLng) => void;
  /** pans the map whenever this value changes */
  panTo?: LatLng | null;
  onMarkerClick?: (id: string) => void;
  fitToMarkers?: boolean;
  interactive?: boolean;
  className?: string;
  showZoomControls?: boolean;
}

const PIN_COLORS: Record<string, string> = {
  me: 'yd-pin--me',
  customer: 'yd-pin--customer',
  pending: 'yd-pin--pending',
  on_the_way: 'yd-pin--on_the_way',
  delivered: 'yd-pin--delivered',
  canceled: 'yd-pin--canceled',
};

function buildIcon(marker: MapMarker) {
  const key =
    marker.kind === 'me'
      ? 'me'
      : marker.status
        ? PIN_COLORS[marker.status] ?? 'customer'
        : 'customer';
  const glyph = marker.kind === 'me' ? 'me' : 'customer';
  const html = `<div class="yd-pin ${key} ${marker.active ? 'yd-pin--active' : ''}">
      ${glyph === 'me' ? '<span class="yd-pin__pulse"></span>' : ''}
      <span class="yd-pin__inner">
        ${
          glyph === 'me'
            ? '<svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v3"/><path d="M12 19v3"/><path d="M2 12h3"/><path d="M19 12h3"/><circle cx="12" cy="12" r="4"/></svg>'
            : '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="2.6"/></svg>'
        }
      </span>
      ${marker.label ? `<span class="yd-pin__label">${escapeHtml(marker.label)}</span>` : ''}
    </div>`;

  return L.divIcon({
    html,
    className: 'yd-pin-wrapper',
    iconSize: [36, 36],
    iconAnchor: [18, 18],
    popupAnchor: [0, -18],
  });
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function MapEvents({ onMapClick }: { onMapClick?: (position: LatLng) => void }) {
  useMapEvents({
    click(event) {
      onMapClick?.({ lat: event.latlng.lat, lng: event.latlng.lng });
    },
  });
  return null;
}

function FitBounds({
  points,
  enabled,
}: {
  points: LatLng[];
  enabled: boolean;
}) {
  const map = useMap();

  useEffect(() => {
    if (!enabled || points.length === 0) return;
    const bounds = L.latLngBounds(points.map((point) => [point.lat, point.lng] as [number, number]));
    if (points.length === 1) {
      map.setView(bounds.getCenter(), 16, { animate: true });
      return;
    }
    map.fitBounds(bounds, { padding: [44, 44], maxZoom: 16.5, animate: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, map, JSON.stringify(points)]);

  return null;
}

function PanTo({ target }: { target?: LatLng | null }) {
  const map = useMap();
  useEffect(() => {
    if (!target) return;
    map.setView([target.lat, target.lng], Math.max(map.getZoom(), 15), { animate: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target?.lat, target?.lng, map]);
  return null;
}

function InvalidateOnMount() {
  const map = useMap();
  useEffect(() => {
    const timer = setTimeout(() => map.invalidateSize(), 120);
    const onResize = () => map.invalidateSize();
    window.addEventListener('resize', onResize);
    return () => {
      clearTimeout(timer);
      window.removeEventListener('resize', onResize);
    };
  }, [map]);
  return null;
}

export default function LeafletMap({
  center,
  zoom = 14,
  markers = [],
  me = null,
  route = null,
  routeDash = false,
  onMapClick,
  onMarkerClick,
  panTo = null,
  fitToMarkers = true,
  interactive = true,
  className = 'h-full w-full',
  showZoomControls = false,
}: LeafletMapProps) {
  const allPoints = useMemo(() => {
    const points = markers.map((marker) => marker.position);
    if (me) points.push(me);
    return points;
  }, [markers, me]);

  const initialCenter = center ?? me ?? allPoints[0] ?? { lat: 35.5556, lng: 45.4351 };

  const icons = useMemo(() => {
    const map = new Map<string, L.DivIcon>();
    for (const marker of markers) map.set(marker.id, buildIcon(marker));
    return map;
  }, [markers]);

  return (
    <MapContainer
      center={[initialCenter.lat, initialCenter.lng]}
      zoom={zoom}
      className={className}
      zoomControl={showZoomControls}
      scrollWheelZoom={interactive}
      dragging={interactive}
      doubleClickZoom={interactive}
      attributionControl
      preferCanvas
    >
      <TileLayer
        url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">OpenStreetMap</a> &middot; <a href="https://carto.com/attributions" target="_blank" rel="noreferrer">CARTO</a>'
        subdomains={['a', 'b', 'c', 'd']}
        maxZoom={20}
        detectRetina
      />

      {route && route.length > 1 && (
        <>
          <Polyline
            positions={route}
            pathOptions={{
              color: '#06B6D4',
              weight: 10,
              opacity: 0.18,
              lineCap: 'round',
              lineJoin: 'round',
            }}
          />
          <Polyline
            positions={route}
            pathOptions={{
              color: '#22D3EE',
              weight: 4,
              opacity: 0.95,
              lineCap: 'round',
              lineJoin: 'round',
              dashArray: routeDash ? '6 10' : undefined,
            }}
          />
        </>
      )}

      {markers.map((marker) => (
        <Marker
          key={marker.id}
          position={[marker.position.lat, marker.position.lng]}
          icon={icons.get(marker.id)}
          eventHandlers={
            onMarkerClick
              ? {
                  click: () => onMarkerClick(marker.id),
                }
              : undefined
          }
        >
          {marker.label && (
            <Tooltip direction="top" offset={[0, -18]} opacity={1}>
              {marker.label}
            </Tooltip>
          )}
        </Marker>
      ))}

      {me && !markers.some((marker) => marker.kind === 'me') && (
        <Marker
          position={[me.lat, me.lng]}
          icon={L.divIcon({
            html: `<div class="yd-pin yd-pin--me"><span class="yd-pin__pulse"></span><span class="yd-pin__inner"><svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v3"/><path d="M12 19v3"/><path d="M2 12h3"/><path d="M19 12h3"/><circle cx="12" cy="12" r="4"/></svg></span></div>`,
            className: 'yd-pin-wrapper',
            iconSize: [36, 36],
            iconAnchor: [18, 18],
          })}
        >
          <Tooltip direction="top" offset={[0, -18]} opacity={1}>
            {t.myLiveLocation}
          </Tooltip>
        </Marker>
      )}

      <MapEvents onMapClick={onMapClick} />
      <PanTo target={panTo} />
      <FitBounds points={allPoints} enabled={fitToMarkers} />
      <InvalidateOnMount />
    </MapContainer>
  );
}
