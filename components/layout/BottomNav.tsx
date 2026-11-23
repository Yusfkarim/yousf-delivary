'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { BarChart3, Home, Map, Plus, Settings } from 'lucide-react';
import clsx from 'clsx';
import { t } from '@/lib/i18n';

const TABS = [
  { href: '/', label: t.navHome, icon: Home },
  { href: '/map', label: t.navMap, icon: Map },
  { href: '/stats', label: t.navStats, icon: BarChart3 },
  { href: '/settings', label: t.navSettings, icon: Settings },
] as const;

export function BottomNav() {
  const pathname = usePathname();
  const isActive = (href: string) =>
    href === '/' ? pathname === '/' : pathname === href || pathname.startsWith(`${href}/`);

  const before = TABS.slice(0, 2);
  const after = TABS.slice(2);

  const renderTab = (tab: (typeof TABS)[number]) => {
    const active = isActive(tab.href);
    const Icon = tab.icon;
    return (
      <Link
        key={tab.href}
        href={tab.href}
        aria-current={active ? 'page' : undefined}
        className={clsx(
          'group relative flex flex-1 flex-col items-center gap-1 rounded-2xl px-1 py-1.5 text-[10.5px] font-medium transition',
          active ? 'text-neon-soft' : 'text-slate-500 hover:text-slate-300',
        )}
      >
        <span
          className={clsx(
            'absolute -top-2 h-1 w-8 rounded-full transition-all',
            active ? 'bg-neon shadow-[0_0_14px_2px_rgba(6,182,212,0.65)]' : 'bg-transparent',
          )}
        />
        <Icon className={clsx('h-[22px] w-[22px] transition-transform', active && 'scale-105')} aria-hidden />
        <span>{tab.label}</span>
      </Link>
    );
  };

  return (
    <nav
      className="fixed inset-x-0 bottom-0 z-50 border-t border-white/10 bg-ink-950/85 backdrop-blur-2xl"
      aria-label="گەڕانی سەرەکی"
    >
      <div className="mx-auto flex max-w-lg items-end gap-0.5 px-2 pb-[max(0.4rem,env(safe-area-inset-bottom))] pt-2 sm:max-w-2xl">
        {before.map(renderTab)}

        <div className="flex w-16 shrink-0 justify-center">
          <Link
            href="/orders/new"
            aria-label={t.newOrder}
            className="-mt-7 grid h-14 w-14 place-items-center rounded-2xl border border-neon/40 bg-gradient-to-br from-neon-soft via-neon to-neon-deep text-ink-950 shadow-[0_16px_40px_-14px_rgba(6,182,212,0.9)] transition active:scale-95"
          >
            <Plus className="h-7 w-7" strokeWidth={2.6} aria-hidden />
          </Link>
        </div>

        {after.map(renderTab)}
      </div>
    </nav>
  );
}
