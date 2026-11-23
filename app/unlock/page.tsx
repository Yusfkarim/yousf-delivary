'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { KeyRound, Loader2, LockKeyhole, Unlock } from 'lucide-react';
import { t } from '@/lib/i18n';

export default function UnlockPage() {
  const router = useRouter();
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (busy || pin.length < 4) return;

    setBusy(true);
    setError(null);

    try {
      const response = await fetch('/api/unlock', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin }),
      });

      if (!response.ok) {
        const payload = (await response.json().catch(() => null)) as { message?: string } | null;
        setError(payload?.message ?? t.unlockError);
        setBusy(false);
        return;
      }

      const next = new URLSearchParams(window.location.search).get('next');
      router.replace(next && next.startsWith('/') ? next : '/');
      router.refresh();
    } catch {
      setError(t.networkError);
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-[70dvh] flex-col items-center justify-center">
      <form onSubmit={submit} className="glass w-full max-w-sm space-y-4 p-5 text-center">
        <span className="mx-auto grid h-16 w-16 place-items-center rounded-3xl border border-neon/30 bg-gradient-to-br from-neon/20 via-ink-800 to-gold/10 text-neon-soft shadow-neon">
          <LockKeyhole className="h-7 w-7" aria-hidden />
        </span>

        <div className="space-y-1">
          <h1 className="text-[16px] font-bold text-white">{t.unlockTitle}</h1>
          <p className="text-[11.5px] leading-6 text-slate-400">{t.unlockHint}</p>
        </div>

        <div className="relative">
          <KeyRound className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" aria-hidden />
          <input
            type="password"
            inputMode="numeric"
            autoComplete="one-time-code"
            autoFocus
            maxLength={10}
            value={pin}
            onChange={(event) => {
              setPin(event.target.value.replace(/\D/g, ''));
              setError(null);
            }}
            placeholder="••••"
            aria-label={t.unlockTitle}
            className="field-input py-3 text-center text-lg tracking-[0.5em]"
          />
        </div>

        {error && <p className="text-[11.5px] text-danger">{error}</p>}

        <button type="submit" disabled={busy || pin.length < 4} className="btn-primary w-full py-2.5 text-[13px]">
          {busy ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden /> : <Unlock className="h-4 w-4" aria-hidden />}
          {busy ? t.loading : t.unlockButton}
        </button>

        <p className="text-[9.5px] text-slate-600">{t.appName}</p>
      </form>
    </div>
  );
}
