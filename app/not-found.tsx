import Link from 'next/link';
import { Compass, Home } from 'lucide-react';
import { t } from '@/lib/i18n';

export default function NotFound() {
  return (
    <div className="flex min-h-[60dvh] flex-col items-center justify-center gap-4 text-center">
      <span className="grid h-20 w-20 place-items-center rounded-3xl border border-neon/25 bg-gradient-to-br from-neon/15 via-ink-800 to-gold/10 text-neon-soft">
        <Compass className="h-8 w-8" aria-hidden />
      </span>
      <div className="space-y-1.5">
        <h1 className="text-[17px] font-bold text-white">{t.notFound}</h1>
        <p className="max-w-xs text-[12px] leading-6 text-slate-400">{t.searchEmptyBody}</p>
      </div>
      <Link href="/" className="btn-primary px-6 py-2.5 text-[12.5px]">
        <Home className="h-4 w-4" aria-hidden />
        {t.navHome}
      </Link>
    </div>
  );
}
