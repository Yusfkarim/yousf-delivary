'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import {
  Banknote,
  BarChart3,
  CheckCheck,
  Clock,
  MapPin,
  PackageCheck,
  Sparkles,
  TrendingUp,
  Wallet,
} from 'lucide-react';
import clsx from 'clsx';
import { useStore } from '@/lib/store';
import { useToast } from '@/components/ui/Toast';
import { EmptyState } from '@/components/ui/EmptyState';
import { daysAgo, dailySeries, startOfDay, summarize, topAreas } from '@/lib/stats';
import { formatCount, formatMoney, relativeTime } from '@/lib/format';
import { t } from '@/lib/i18n';

type Period = 'today' | 'week' | 'all';

const PERIODS: Array<{ value: Period; label: string }> = [
  { value: 'today', label: t.periodToday },
  { value: 'week', label: t.periodWeek },
  { value: 'all', label: t.periodAll },
];

const WEEKDAY_SHORT = ['یەک', 'دوو', 'سێ', 'چوا', 'پێن', 'هەی', 'شەم'];

export default function StatsPage() {
  const { deliveries, settings, seedDemo, backend } = useStore();
  const { toast } = useToast();
  const [period, setPeriod] = useState<Period>('today');
  const [seeding, setSeeding] = useState(false);
  const kurdish = settings.kurdishDigits;

  const since = useMemo(() => {
    if (period === 'today') return startOfDay();
    if (period === 'week') return daysAgo(6);
    return null;
  }, [period]);

  const summary = useMemo(() => summarize(deliveries, since), [deliveries, since]);
  const series = useMemo(() => dailySeries(deliveries, 7), [deliveries]);
  const areas = useMemo(() => topAreas(deliveries, 4), [deliveries]);
  const maxDaily = Math.max(1, ...series.map((point) => point.count));

  const recent = useMemo(
    () =>
      deliveries
        .filter((item) => item.status === 'delivered')
        .sort(
          (a, b) =>
            new Date(b.deliveredAt ?? b.createdAt).getTime() -
            new Date(a.deliveredAt ?? a.createdAt).getTime(),
        )
        .slice(0, 5),
    [deliveries],
  );

  const successRate =
    summary.delivered + summary.canceled > 0
      ? Math.round((summary.delivered / (summary.delivered + summary.canceled)) * 100)
      : 0;

  const handleSeed = async () => {
    setSeeding(true);
    const result = await seedDemo();
    setSeeding(false);
    toast(result.ok ? t.demoLoaded : result.message, result.ok ? 'success' : 'error');
  };

  const metrics = [
    {
      label: t.todayDelivered,
      value: formatCount(summary.delivered, kurdish),
      suffix: t.deliveriesUnit,
      icon: CheckCheck,
      tone: 'text-success',
      border: 'border-success/20',
    },
    {
      label: t.deliveryFees,
      value: formatMoney(summary.fees, 'IQD', { kurdish, compact: true }),
      suffix: '',
      icon: Wallet,
      tone: 'text-neon-soft',
      border: 'border-neon/20',
    },
    {
      label: t.avgFee,
      value: formatMoney(summary.averageFee, 'IQD', { kurdish, compact: true }),
      suffix: t.perOrder,
      icon: TrendingUp,
      tone: 'text-gold-soft',
      border: 'border-gold/20',
    },
    {
      label: t.pendingOrders,
      value: formatCount(summary.pending + summary.onTheWay, kurdish),
      suffix: t.ordersUnit,
      icon: Clock,
      tone: 'text-gold-soft',
      border: 'border-gold/20',
    },
  ];

  return (
    <div className="space-y-3.5 pt-1">
      <div className="flex items-center gap-2">
        <div>
          <h1 className="text-[15px] font-bold text-white">{t.statsTitle}</h1>
          <p className="text-[10.5px] text-slate-500">{t.statsSubtitle}</p>
        </div>
        <span className="mr-auto chip chip-neon text-[10px]">
          <BarChart3 className="h-3 w-3" aria-hidden />
          {formatCount(deliveries.length, kurdish)} {t.orderSingular}
        </span>
      </div>

      <div className="grid grid-cols-3 gap-1.5">
        {PERIODS.map((item) => (
          <button
            key={item.value}
            type="button"
            onClick={() => setPeriod(item.value)}
            aria-pressed={period === item.value}
            className={clsx(
              'rounded-2xl border py-2 text-[11.5px] font-semibold transition active:scale-95',
              period === item.value
                ? 'border-neon/45 bg-neon/15 text-neon-soft'
                : 'border-white/10 bg-white/[0.03] text-slate-400',
            )}
          >
            {item.label}
          </button>
        ))}
      </div>

      <section className="glass relative overflow-hidden p-4">
        <span className="pointer-events-none absolute -left-16 -top-16 h-40 w-40 rounded-full bg-gold/15 blur-3xl" />
        <span className="pointer-events-none absolute -bottom-20 -right-10 h-40 w-40 rounded-full bg-neon/10 blur-3xl" />
        <p className="text-[11px] font-medium text-slate-400">{t.cashCollected}</p>
        <p className="mt-1 text-[26px] font-bold leading-tight text-white">
          {formatMoney(summary.cash, 'IQD', { kurdish })}
        </p>
        <div className="mt-2 flex flex-wrap items-center gap-1.5">
          <span className="chip chip-success">
            <CheckCheck className="h-3 w-3" aria-hidden />
            {formatCount(summary.delivered, kurdish)} {t.deliveriesUnit}
          </span>
          <span className="chip chip-gold">
            <Banknote className="h-3 w-3" aria-hidden />
            {t.deliveryFees}: {formatMoney(summary.fees, 'IQD', { kurdish, compact: true })}
          </span>
          <span className="chip">
            <TrendingUp className="h-3 w-3" aria-hidden />
            {t.successRate}: {formatCount(successRate, kurdish)}%
          </span>
        </div>
      </section>

      <div className="grid grid-cols-2 gap-2">
        {metrics.map((metric) => {
          const Icon = metric.icon;
          return (
            <div key={metric.label} className={clsx('glass-soft flex items-start gap-2.5 p-3', metric.border)}>
              <span className={clsx('grid h-8 w-8 shrink-0 place-items-center rounded-xl border border-white/10 bg-white/[0.04]', metric.tone)}>
                <Icon className="h-4 w-4" aria-hidden />
              </span>
              <div className="min-w-0">
                <p className="text-[10px] leading-4 text-slate-400">{metric.label}</p>
                <p className="truncate text-[14px] font-bold leading-6 text-white">{metric.value}</p>
                {metric.suffix && <p className="text-[9px] text-slate-500">{metric.suffix}</p>}
              </div>
            </div>
          );
        })}
      </div>

      <section className="glass p-3.5">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="flex items-center gap-1.5 text-[12.5px] font-semibold text-slate-200">
            <BarChart3 className="h-4 w-4 text-neon-soft" aria-hidden />
            {t.last7Days}
          </h2>
          <span className="text-[10px] text-slate-500">{t.weekdayDeliveries}</span>
        </div>

        <div className="flex h-32 items-end justify-between gap-1.5">
          {series.map((point) => {
            const height = point.count === 0 ? 6 : Math.max(12, Math.round((point.count / maxDaily) * 100));
            return (
              <div key={point.date.toISOString()} className="flex flex-1 flex-col items-center gap-1.5">
                <span className="text-[9px] font-semibold text-slate-400">
                  {point.count > 0 ? formatCount(point.count, kurdish) : ''}
                </span>
                <div className="flex w-full flex-1 items-end">
                  <div
                    className={clsx(
                      'w-full rounded-t-lg transition-all',
                      point.count > 0
                        ? 'bg-gradient-to-t from-neon-deep/60 to-neon-soft'
                        : 'bg-white/[0.06]',
                    )}
                    style={{ height: `${height}%` }}
                    title={`${point.count}`}
                  />
                </div>
                <span className="text-[9px] text-slate-500">{WEEKDAY_SHORT[point.date.getDay()]}</span>
              </div>
            );
          })}
        </div>
      </section>

      <section className="glass p-3.5">
        <h2 className="mb-2 flex items-center gap-1.5 text-[12.5px] font-semibold text-slate-200">
          <MapPin className="h-4 w-4 text-gold-soft" aria-hidden />
          {t.topAreas}
        </h2>
        {areas.length === 0 ? (
          <p className="py-2 text-[11px] text-slate-500">{t.noAreasYet}</p>
        ) : (
          <ul className="space-y-2">
            {areas.map((area, index) => {
              const width = Math.max(8, Math.round((area.count / areas[0].count) * 100));
              return (
                <li key={area.area}>
                  <div className="mb-1 flex items-center justify-between text-[11px]">
                    <span className="truncate text-slate-300">
                      {formatCount(index + 1, kurdish)}. {area.area}
                    </span>
                    <span className="text-slate-500">
                      {formatCount(area.count, kurdish)} {t.orderSingular}
                    </span>
                  </div>
                  <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/[0.06]">
                    <div
                      className="h-full rounded-full bg-gradient-to-l from-gold-soft to-gold-deep"
                      style={{ width: `${width}%` }}
                    />
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {deliveries.length > 0 && (
      <section className="glass p-3.5">
        <h2 className="mb-2 flex items-center gap-1.5 text-[12.5px] font-semibold text-slate-200">
          <PackageCheck className="h-4 w-4 text-success" aria-hidden />
          {t.recentActivity}
        </h2>
        {recent.length === 0 ? (
          <div className="py-2">
            <p className="text-[11px] text-slate-500">{t.emptyStats}</p>
            {backend === 'postgres' && (
              <button
                type="button"
                onClick={() => void handleSeed()}
                disabled={seeding}
                className="mt-2 inline-flex items-center gap-1.5 text-[11px] text-neon-soft underline decoration-dotted"
              >
                <Sparkles className="h-3 w-3" aria-hidden />
                {t.loadDemo}
              </button>
            )}
          </div>
        ) : (
          <ul className="divide-y divide-white/[0.05]">
            {recent.map((item) => (
              <li key={item.id}>
                <Link href={`/orders/${item.id}`} className="flex items-center gap-2 py-2">
                  <span className="grid h-7 w-7 shrink-0 place-items-center rounded-lg border border-success/25 bg-success/10 text-success">
                    <CheckCheck className="h-3.5 w-3.5" aria-hidden />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-[12px] text-slate-200">{item.name}</span>
                    <span className="block truncate text-[10px] text-slate-500">
                      {relativeTime(item.deliveredAt ?? item.createdAt, kurdish)}
                    </span>
                  </span>
                  <span className="shrink-0 text-[11.5px] font-semibold text-gold-soft">
                    {formatMoney(item.price + item.deliveryFee, item.currency, { kurdish, compact: true })}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
      )}

      {deliveries.length === 0 && (
        <EmptyState
          icon={<BarChart3 className="h-7 w-7" />}
          title={t.emptyStats}
          body={t.emptyBody}
          action={
            <Link href="/orders/new" className="btn-primary">
              {t.emptyCta}
            </Link>
          }
        />
      )}
    </div>
  );
}
