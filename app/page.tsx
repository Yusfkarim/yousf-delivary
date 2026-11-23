'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import {
  AlertTriangle,
  ArrowUpDown,
  Moon,
  PackageOpen,
  Plus,
  SearchX,
  Sparkles,
  Sun,
  Sunrise,
} from 'lucide-react';
import { useStore } from '@/lib/store';
import { StatsBar } from '@/components/StatsBar';
import { SearchBar } from '@/components/SearchBar';
import { FilterPills } from '@/components/FilterPills';
import { DeliveryCard } from '@/components/DeliveryCard';
import { RouteModal } from '@/components/RouteModal';
import { EmptyState } from '@/components/ui/EmptyState';
import { SkeletonCard } from '@/components/ui/SkeletonCard';
import { useToast } from '@/components/ui/Toast';
import { applyFilters } from '@/lib/search';
import { t } from '@/lib/i18n';
import type { Delivery, SortMode, StatusFilter } from '@/lib/types';
import { formatCount, formatDate, weekdayName } from '@/lib/format';

export default function HomePage() {
  const { deliveries, loading, error, position, settings, refresh, seedDemo, backend } = useStore();
  const { toast } = useToast();
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<StatusFilter>('all');
  const [sort, setSort] = useState<SortMode>('nearest');
  const [active, setActive] = useState<Delivery | null>(null);
  const [seeding, setSeeding] = useState(false);

  // ⌘K / Ctrl+K focuses the search field.
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        document.querySelector<HTMLInputElement>('input[type="search"]')?.focus();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const counts = useMemo(
    () => ({
      all: deliveries.length,
      pending: deliveries.filter((item) => item.status === 'pending').length,
      on_the_way: deliveries.filter((item) => item.status === 'on_the_way').length,
      delivered: deliveries.filter((item) => item.status === 'delivered').length,
    }),
    [deliveries],
  );

  const visible = useMemo(
    () => applyFilters(deliveries, { query, status, sort, position }),
    [deliveries, query, status, sort, position],
  );

  const activeDeliveries = visible.filter((item) => item.status !== 'delivered' && item.status !== 'canceled');
  const finishedDeliveries = visible.filter((item) => item.status === 'delivered' || item.status === 'canceled');

  const handleSeed = async () => {
    setSeeding(true);
    const result = await seedDemo();
    setSeeding(false);
    toast(result.ok ? t.demoLoaded : result.message, result.ok ? 'success' : 'error');
  };

  // Rendered after mount only: the server is in UTC, the driver is in Iraq.
  const [now, setNow] = useState<Date | null>(null);
  useEffect(() => {
    setNow(new Date());
    const timer = setInterval(() => setNow(new Date()), 60_000);
    return () => clearInterval(timer);
  }, []);

  const hour = now?.getHours() ?? 8;
  const GreetingIcon = hour < 5 || hour >= 18 ? Moon : hour < 11 ? Sunrise : Sun;

  return (
    <div className="space-y-3.5 pt-1">
      <div className="flex items-center gap-2.5 px-1 animate-fade-up">
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl border border-gold/25 bg-gradient-to-br from-gold/15 to-neon/5 text-gold-soft">
          <GreetingIcon className="h-4 w-4" aria-hidden />
        </span>
        <div className="min-w-0">
          <p className="text-[10px] text-slate-500">
            {now ? `${weekdayName(now)} · ${formatDate(now, settings.kurdishDigits)}` : t.today}
          </p>
          <h1 className="truncate text-[16px] font-bold text-white">
            {t.greeting(settings.driverName)}
          </h1>
        </div>
      </div>

      <StatsBar />
      <SearchBar value={query} onChange={setQuery} resultCount={visible.length} />
      <FilterPills
        status={status}
        onStatusChange={setStatus}
        sort={sort}
        onSortChange={setSort}
        counts={counts}
      />

      {error && deliveries.length > 0 && (
        <div className="flex items-center gap-2 rounded-2xl border border-danger/25 bg-danger/10 px-3 py-2 text-[11.5px] text-danger">
          <AlertTriangle className="h-3.5 w-3.5 shrink-0" aria-hidden />
          <p className="flex-1">{error}</p>
          <button type="button" onClick={() => void refresh()} className="font-semibold underline">
            {t.retry}
          </button>
        </div>
      )}

      {loading && deliveries.length === 0 ? (
        <div className="space-y-3">
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : deliveries.length === 0 ? (
        error ? (
          <EmptyState
            icon={<AlertTriangle className="h-7 w-7" />}
            title={t.error}
            body={error}
            action={
              <button type="button" onClick={() => void refresh()} className="btn-ghost">
                {t.retry}
              </button>
            }
          />
        ) : (
          <EmptyState
            icon={<PackageOpen className="h-7 w-7" />}
            title={t.emptyTitle}
            body={t.emptyBody}
            action={
              <div className="flex flex-col items-center gap-2">
                <Link href="/orders/new" className="btn-primary">
                  <Plus className="h-4 w-4" aria-hidden />
                  {t.emptyCta}
                </Link>
                {backend === 'postgres' && (
                  <button
                    type="button"
                    onClick={() => void handleSeed()}
                    disabled={seeding}
                    className="inline-flex items-center gap-1.5 text-[11px] text-slate-400 underline decoration-dotted"
                  >
                    <Sparkles className="h-3 w-3" aria-hidden />
                    {t.loadDemo}
                  </button>
                )}
              </div>
            }
          />
        )
      ) : visible.length === 0 ? (
        <EmptyState
          icon={<SearchX className="h-7 w-7" />}
          title={query ? t.searchEmptyTitle : t.emptyFilteredTitle}
          body={query ? t.searchEmptyBody : t.emptyFilteredBody}
          action={
            <button
              type="button"
              onClick={() => {
                setQuery('');
                setStatus('all');
              }}
              className="btn-ghost"
            >
              {t.filterAll}
            </button>
          }
        />
      ) : (
        <>
          <div className="flex items-center gap-2 px-1 pt-0.5">
            <span className="inline-flex items-center gap-1.5 text-[11px] font-medium text-slate-400">
              <ArrowUpDown className="h-3 w-3" aria-hidden />
              {sort === 'nearest' ? t.sortNearest : t.sortNewest}
            </span>
            <span className="mr-auto text-[11px] text-slate-500">
              {formatCount(visible.length, settings.kurdishDigits)} {t.resultsCount}
            </span>
          </div>

          <div className="space-y-3">
            {activeDeliveries.map((delivery, index) => (
              <DeliveryCard
                key={delivery.id}
                delivery={delivery}
                index={index}
                onShowRoute={setActive}
              />
            ))}
          </div>

          {finishedDeliveries.length > 0 && (
            <div className="pt-2">
              <div className="mb-2 flex items-center gap-2 px-1">
                <span className="h-px flex-1 bg-gradient-to-l from-transparent via-white/10 to-transparent" />
                <span className="text-[11px] font-medium text-slate-500">
                  {t.filterDelivered} · {formatCount(finishedDeliveries.length, settings.kurdishDigits)}
                </span>
                <span className="h-px flex-1 bg-gradient-to-r from-transparent via-white/10 to-transparent" />
              </div>
              <div className="space-y-3 opacity-90">
                {finishedDeliveries.map((delivery, index) => (
                  <DeliveryCard
                    key={delivery.id}
                    delivery={delivery}
                    index={index}
                    onShowRoute={setActive}
                  />
                ))}
              </div>
            </div>
          )}
        </>
      )}

      <RouteModal delivery={active} onClose={() => setActive(null)} />
    </div>
  );
}
