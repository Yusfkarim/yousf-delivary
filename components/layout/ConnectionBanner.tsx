'use client';

import { CloudOff, RefreshCw } from 'lucide-react';
import { t } from '@/lib/i18n';
import { useStore } from '@/lib/store';

export function ConnectionBanner() {
  const { online, outboxCount, flushOutbox } = useStore();

  if (online && outboxCount === 0) return null;

  return (
    <div className="mx-auto w-full max-w-lg px-4 pt-2 sm:max-w-2xl">
      <div className="flex items-center gap-2.5 rounded-2xl border border-gold/30 bg-gold/10 px-3.5 py-2.5 text-[12px] text-gold-soft backdrop-blur-xl">
        <CloudOff className="h-4 w-4 shrink-0" aria-hidden />
        <p className="flex-1 leading-5">
          {online ? t.syncPendingNotice : t.offlineCacheNotice}
          {outboxCount > 0 && <span className="font-semibold"> ({outboxCount})</span>}
        </p>
        {online && outboxCount > 0 && (
          <button
            type="button"
            onClick={() => void flushOutbox()}
            className="inline-flex items-center gap-1 rounded-full border border-gold/40 bg-gold/15 px-2.5 py-1 text-[11px] font-semibold active:scale-95"
          >
            <RefreshCw className="h-3 w-3" aria-hidden />
            {t.syncNow}
          </button>
        )}
      </div>
    </div>
  );
}
