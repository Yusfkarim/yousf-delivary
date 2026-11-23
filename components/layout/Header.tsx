'use client';

import Link from 'next/link';
import { Loader2, MapPin, Settings, Wifi, WifiOff } from 'lucide-react';
import clsx from 'clsx';
import { t } from '@/lib/i18n';
import { useStore } from '@/lib/store';

function GpsPill() {
  const { locationState, requestPosition, position } = useStore();
  const ready = locationState === 'ready' && Boolean(position);
  const locating = locationState === 'locating';

  return (
    <button
      type="button"
      onClick={requestPosition}
      title={ready ? t.locationActive : t.enableLocation}
      className={clsx(
        'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1.5 text-[11px] font-semibold leading-none transition active:scale-95',
        ready
          ? 'border-neon/35 bg-neon/10 text-neon-soft'
          : 'border-gold/40 bg-gold/10 text-gold-soft',
      )}
    >
      {locating ? (
        <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden />
      ) : (
        <span className="relative flex h-1.5 w-1.5">
          <span
            className={clsx(
              'absolute inline-flex h-full w-full rounded-full',
              ready ? 'animate-ping bg-neon/70' : 'bg-gold/70',
            )}
          />
          <span
            className={clsx(
              'relative inline-flex h-1.5 w-1.5 rounded-full',
              ready ? 'bg-neon' : 'bg-gold',
            )}
          />
        </span>
      )}
      <span>{ready ? 'GPS' : locating ? '…' : 'GPS ✕'}</span>
    </button>
  );
}

export function Header() {
  const { online, outboxCount } = useStore();

  return (
    <header className="sticky top-0 z-40 border-b border-white/[0.06] bg-ink-900/75 pb-2.5 pt-[max(0.6rem,env(safe-area-inset-top))] backdrop-blur-2xl">
      <div className="mx-auto flex max-w-lg items-center gap-3 px-4 sm:max-w-2xl">
        <Link href="/" className="flex min-w-0 items-center gap-2.5" aria-label={t.appName}>
          <span className="relative grid h-11 w-11 shrink-0 place-items-center rounded-2xl border border-white/10 bg-gradient-to-br from-neon/25 via-ink-800 to-gold/10 shadow-[0_10px_30px_-12px_rgba(6,182,212,0.7)]">
            <MapPin className="h-5 w-5 text-neon-soft" strokeWidth={2.2} aria-hidden />
            <span className="absolute -bottom-0.5 -left-0.5 h-2.5 w-2.5 rounded-full border-2 border-ink-900 bg-gold" />
          </span>
          <span className="min-w-0">
            <span className="block truncate text-[15px] font-bold leading-tight text-white">
              {t.appName}
            </span>
            <span className="block truncate text-[10.5px] leading-tight text-slate-400">
              {t.appTagline}
            </span>
          </span>
        </Link>

        <div className="mr-auto flex shrink-0 items-center gap-1.5">
          {outboxCount > 0 && (
            <span className="inline-flex items-center gap-1 rounded-full border border-gold/35 bg-gold/10 px-2 py-1 text-[10px] font-semibold text-gold-soft">
              {outboxCount}
            </span>
          )}
          <span
            className={clsx(
              'inline-flex h-7 w-7 items-center justify-center rounded-full border',
              online
                ? 'border-success/25 bg-success/10 text-success'
                : 'border-gold/30 bg-gold/10 text-gold-soft',
            )}
            title={online ? t.onlineStatus : t.offlineStatus}
          >
            {online ? <Wifi className="h-3.5 w-3.5" aria-hidden /> : <WifiOff className="h-3.5 w-3.5" aria-hidden />}
          </span>
          <GpsPill />
          <Link
            href="/settings"
            aria-label={t.settingsTitle}
            className="grid h-8 w-8 place-items-center rounded-full border border-white/10 bg-white/[0.04] text-slate-300 transition hover:bg-white/[0.08]"
          >
            <Settings className="h-4 w-4" aria-hidden />
          </Link>
        </div>
      </div>
    </header>
  );
}
