'use client';

import { Search, X } from 'lucide-react';
import { t } from '@/lib/i18n';

interface SearchBarProps {
  value: string;
  onChange: (value: string) => void;
  resultCount?: number;
}

export function SearchBar({ value, onChange, resultCount }: SearchBarProps) {
  return (
    <div className="animate-fade-up">
      <div className="relative">
        <Search
          className="pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500"
          aria-hidden
        />
        <input
          type="search"
          inputMode="search"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={t.searchPlaceholder}
          aria-label={t.searchPlaceholder}
          className="w-full rounded-2xl border border-white/10 bg-ink-950/60 py-3 pr-10 pl-11 text-[14px] text-slate-50 placeholder:text-slate-500 outline-none transition focus:border-neon/60 focus:ring-2 focus:ring-neon/20 [&::-webkit-search-cancel-button]:hidden"
        />
        {value ? (
          <button
            type="button"
            onClick={() => onChange('')}
            aria-label={t.clearSearch}
            className="absolute left-2 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-xl text-slate-400 transition hover:bg-white/5 hover:text-slate-100"
          >
            <X className="h-4 w-4" aria-hidden />
          </button>
        ) : (
          <span className="absolute left-3 top-1/2 hidden -translate-y-1/2 rounded-lg border border-white/10 bg-white/[0.04] px-1.5 py-0.5 text-[9px] font-semibold text-slate-500 sm:inline-block">
            ⌘K
          </span>
        )}
      </div>

      {value && typeof resultCount === 'number' && (
        <p className="mt-1.5 px-1 text-[11px] text-slate-500">
          {t.resultsCount}: <span className="font-semibold text-neon-soft">{resultCount}</span>
        </p>
      )}
    </div>
  );
}
