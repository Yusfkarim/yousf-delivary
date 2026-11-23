'use client';

import { useMemo } from 'react';
import { Banknote, CheckCircle2, Clock, Loader2, RefreshCw } from 'lucide-react';
import clsx from 'clsx';
import { useStore } from '@/lib/store';
import { summarize, startOfDay } from '@/lib/stats';
import { formatCount, formatMoney } from '@/lib/format';
import { t } from '@/lib/i18n';

export function StatsBar() {
  const { deliveries, settings, refresh, refreshing } = useStore();
  const today = useMemo(() => summarize(deliveries, startOfDay()), [deliveries]);
  const kurdish = settings.kurdishDigits;

  return (
    <section aria-label={t.dashboardTitle} className="animate-fade-up">
      <div className="mb-2 flex items-center gap-2 px-0.5">
        <h2 className="text-[13px] font-bold text-slate-200">{t.dashboardTitle}</h2>
        <button
          type="button"
          onClick={() => void refresh()}
          aria-label={t.refresh}
          className="mr-auto grid h-8 w-8 place-items-center rounded-xl border border-white/10 bg-white/[0.04] text-slate-300 transition active:scale-95"
        >
          {refreshing ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin text-neon" aria-hidden />
          ) : (
            <RefreshCw className="h-3.5 w-3.5" aria-hidden />
          )}
        </button>
      </div>

      <div className="grid grid-cols-3 gap-2">
        <div className="stat-card">
          <span className="flex items-center gap-1.5 text-[10.5px] font-medium text-success">
            <CheckCircle2 className="h-3.5 w-3.5" aria-hidden />
            {t.todayDelivered}
          </span>
          <span className="text-[17px] font-bold leading-tight text-white">
            {formatCount(today.delivered, kurdish)}
          </span>
        </div>

        <div className="stat-card">
          <span className="flex items-center gap-1.5 text-[10.5px] font-medium text-gold-soft">
            <Clock className="h-3.5 w-3.5" aria-hidden />
            {t.pendingOrders}
          </span>
          <span className="text-[17px] font-bold leading-tight text-white">
            {formatCount(today.pending + today.onTheWay, kurdish)}
          </span>
        </div>

        <div
          className={clsx(
            'stat-card relative overflow-hidden border-gold/25 bg-gradient-to-br from-gold/15 via-white/[0.03] to-transparent',
          )}
        >
          <span className="flex items-center gap-1.5 text-[10.5px] font-medium text-gold-soft">
            <Banknote className="h-3.5 w-3.5" aria-hidden />
            {t.cashCollected}
          </span>
          <span className="truncate text-[14px] font-bold leading-tight text-gold-soft">
            {formatMoney(today.cash, 'IQD', { kurdish, compact: true })}
          </span>
        </div>
      </div>
    </section>
  );
}
