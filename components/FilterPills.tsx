'use client';

import { ArrowDownUp, LocateFixed, Navigation, Sparkles } from 'lucide-react';
import clsx from 'clsx';
import { t } from '@/lib/i18n';
import type { SortMode, StatusFilter } from '@/lib/types';
import { useStore } from '@/lib/store';
import { useToast } from '@/components/ui/Toast';

const STATUS_FILTERS: Array<{ value: StatusFilter; label: string }> = [
  { value: 'all', label: t.filterAll },
  { value: 'pending', label: t.filterPending },
  { value: 'on_the_way', label: t.filterOnTheWay },
  { value: 'delivered', label: t.filterDelivered },
];

interface FilterPillsProps {
  status: StatusFilter;
  onStatusChange: (value: StatusFilter) => void;
  sort: SortMode;
  onSortChange: (value: SortMode) => void;
  counts: Record<StatusFilter, number>;
}

export function FilterPills({ status, onStatusChange, sort, onSortChange, counts }: FilterPillsProps) {
  const { position, requestPosition } = useStore();
  const { toast } = useToast();

  return (
    <div className="space-y-2">
      <div className="no-scrollbar -mx-1 flex gap-1.5 overflow-x-auto px-1 pb-0.5">
        {STATUS_FILTERS.map((filter) => {
          const active = status === filter.value;
          return (
            <button
              key={filter.value}
              type="button"
              onClick={() => onStatusChange(filter.value)}
              aria-pressed={active}
              className={clsx('pill', active ? 'pill-active' : 'pill-idle')}
            >
              {filter.label}
              <span
                className={clsx(
                  'rounded-full px-1.5 py-0.5 text-[10px] font-bold',
                  active ? 'bg-neon/25 text-neon-soft' : 'bg-white/10 text-slate-400',
                )}
              >
                {counts[filter.value] ?? 0}
              </span>
            </button>
          );
        })}
      </div>

      <div className="no-scrollbar -mx-1 flex gap-1.5 overflow-x-auto px-1 pb-0.5">
        <button
          type="button"
          onClick={() => {
            if (!position) {
              requestPosition();
              toast(t.sortNeedsGps, 'info');
            }
            onSortChange('nearest');
          }}
          aria-pressed={sort === 'nearest'}
          className={clsx('pill', sort === 'nearest' ? 'pill-active' : 'pill-idle')}
        >
          <Navigation className="h-3.5 w-3.5" aria-hidden />
          {t.sortNearest}
        </button>
        <button
          type="button"
          onClick={() => onSortChange('newest')}
          aria-pressed={sort === 'newest'}
          className={clsx('pill', sort === 'newest' ? 'pill-active' : 'pill-idle')}
        >
          <ArrowDownUp className="h-3.5 w-3.5" aria-hidden />
          {t.sortNewest}
        </button>

        <span className="mr-auto inline-flex shrink-0 items-center gap-1 text-[10.5px] text-slate-500">
          {position ? (
            <>
              <LocateFixed className="h-3 w-3 text-neon" aria-hidden />
              {t.locationActive}
            </>
          ) : (
            <button
              type="button"
              onClick={requestPosition}
              className="inline-flex items-center gap-1 rounded-full border border-gold/30 bg-gold/10 px-2 py-1 font-medium text-gold-soft"
            >
              <Sparkles className="h-3 w-3" aria-hidden />
              {t.enableLocation}
            </button>
          )}
        </span>
      </div>
    </div>
  );
}
