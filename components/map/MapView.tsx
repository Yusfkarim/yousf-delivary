'use client';

import dynamic from 'next/dynamic';
import { Loader2 } from 'lucide-react';
import type { LeafletMapProps } from './LeafletMap';

const LeafletMap = dynamic(() => import('./LeafletMap'), {
  ssr: false,
  loading: () => (
    <div className="grid h-full w-full place-items-center bg-ink-950/60">
      <div className="flex flex-col items-center gap-2 text-slate-400">
        <Loader2 className="h-5 w-5 animate-spin text-neon" aria-hidden />
        <span className="text-[11px]">نەخشە بار دەکرێت…</span>
      </div>
    </div>
  ),
});

export function MapView(props: LeafletMapProps) {
  return <LeafletMap {...props} />;
}
