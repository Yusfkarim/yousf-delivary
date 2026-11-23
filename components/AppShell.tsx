'use client';

import type { ReactNode } from 'react';
import { usePathname } from 'next/navigation';
import { Header } from './layout/Header';
import { BottomNav } from './layout/BottomNav';
import { ConnectionBanner } from './layout/ConnectionBanner';

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();

  if (pathname === '/unlock') {
    return <main className="mx-auto w-full max-w-lg px-4 py-6">{children}</main>;
  }

  return (
    <div className="relative min-h-dvh w-full">
      <Header />
      <ConnectionBanner />
      <main className="page">{children}</main>
      <BottomNav />
    </div>
  );
}
