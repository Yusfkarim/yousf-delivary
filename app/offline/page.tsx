'use client';

import Link from 'next/link';
import { CloudOff, RefreshCw, WifiOff } from 'lucide-react';
import { t } from '@/lib/i18n';

export default function OfflinePage() {
  return (
    <div className="flex min-h-[60dvh] flex-col items-center justify-center gap-4 text-center">
      <span className="relative grid h-20 w-20 place-items-center rounded-3xl border border-gold/25 bg-gradient-to-br from-gold/15 via-ink-800 to-neon/10 text-gold-soft">
        <span className="absolute inset-0 rounded-3xl border border-gold/10" />
        <CloudOff className="h-8 w-8" aria-hidden />
      </span>

      <div className="space-y-1.5">
        <h1 className="text-[17px] font-bold text-white">{t.offlineStatus}</h1>
        <p className="max-w-xs text-[12px] leading-6 text-slate-400">{t.offlineCacheNotice}</p>
      </div>

      <div className="flex flex-col gap-2">
        <button type="button" onClick={() => window.location.reload()} className="btn-primary px-6 py-2.5 text-[12.5px]">
          <RefreshCw className="h-4 w-4" aria-hidden />
          {t.retry}
        </button>
        <Link href="/" className="btn-ghost px-6 py-2.5 text-[12.5px]">
          <WifiOff className="h-4 w-4" aria-hidden />
          {t.navHome}
        </Link>
      </div>
    </div>
  );
}
