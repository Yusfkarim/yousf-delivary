'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  BadgeCheck,
  CircleDollarSign,
  Database,
  Download,
  HardDrive,
  Info,
  Languages,
  Loader2,
  RefreshCw,
  Smartphone,
  Sparkles,
  Trash2,
  User,
  Wifi,
  WifiOff,
} from 'lucide-react';
import clsx from 'clsx';
import { useStore } from '@/lib/store';
import { useToast } from '@/components/ui/Toast';
import { useInstall } from '@/components/pwa/InstallProvider';
import { api } from '@/lib/api';
import { cacheSizeBytes, formatBytes } from '@/lib/cache';
import { t } from '@/lib/i18n';
import { APP_VERSION } from '@/lib/constants';
import type { Currency } from '@/lib/types';

function Toggle({
  checked,
  onChange,
  label,
  hint,
}: {
  checked: boolean;
  onChange: (value: boolean) => void;
  label: string;
  hint?: string;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="flex w-full items-center gap-3 text-right"
    >
      <span className="min-w-0 flex-1">
        <span className="block text-[12.5px] font-medium text-slate-100">{label}</span>
        {hint && <span className="mt-0.5 block text-[10.5px] leading-5 text-slate-500">{hint}</span>}
      </span>
      <span
        className={clsx(
          'relative h-6 w-11 shrink-0 rounded-full border transition',
          checked ? 'border-neon/50 bg-neon/30' : 'border-white/10 bg-white/[0.06]',
        )}
      >
        <span
          className={clsx(
            'absolute right-1 top-1/2 h-4 w-4 -translate-y-1/2 rounded-full transition-all',
            checked
              ? '-translate-x-[20px] bg-neon shadow-[0_0_12px_2px_rgba(6,182,212,0.5)]'
              : 'translate-x-0 bg-slate-400',
          )}
        />
      </span>
    </button>
  );
}

export default function SettingsPage() {
  const {
    settings,
    updateSettings,
    deliveries,
    outboxCount,
    flushOutbox,
    clearLocalData,
    refresh,
    seedDemo,
    online,
    backend,
  } = useStore();
  const { toast } = useToast();
  const install = useInstall();

  const [cacheSize, setCacheSize] = useState(0);
  const [health, setHealth] = useState<{ ok: boolean; backend: string; count?: number } | null>(null);
  const [loadingHealth, setLoadingHealth] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);

  const loadHealth = useCallback(async () => {
    setLoadingHealth(true);
    try {
      const result = await api.health();
      setHealth({ ok: result.ok, backend: result.backend, count: undefined });
    } catch {
      setHealth({ ok: false, backend: backend ?? 'json' });
    } finally {
      setLoadingHealth(false);
    }
  }, [backend]);

  useEffect(() => {
    setCacheSize(cacheSizeBytes());
    void loadHealth();
  }, [loadHealth]);

  const handleSeed = async () => {
    setBusy('seed');
    const result = await seedDemo();
    setBusy(null);
    toast(result.ok ? t.demoLoaded : result.message, result.ok ? 'success' : 'error');
  };

  const handleClear = () => {
    clearLocalData();
    setCacheSize(0);
    // Pull the server copy back in straight away when we are online.
    if (online) void refresh();
  };

  const handleInstall = async () => {
    const outcome = await install.promptInstall();
    if (outcome === 'accepted') toast(t.installApp, 'success');
  };

  return (
    <div className="space-y-3.5 pt-1">
      <div>
        <h1 className="text-[15px] font-bold text-white">{t.settingsTitle}</h1>
        <p className="text-[10.5px] text-slate-500">{t.appTagline}</p>
      </div>

      {/* Driver */}
      <section className="glass space-y-3 p-3.5">
        <h2 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <User className="h-4 w-4 text-neon-soft" aria-hidden />
          {t.driverName}
        </h2>
        <div>
          <input
            value={settings.driverName}
            onChange={(event) => updateSettings({ driverName: event.target.value.slice(0, 40) })}
            placeholder={t.driverNamePlaceholder}
            className="field-input"
          />
          <p className="mt-1 px-1 text-[10px] text-slate-500">{t.driverNameHint}</p>
        </div>
      </section>

      {/* Appearance & defaults */}
      <section className="glass space-y-3 p-3.5">
        <h2 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <Languages className="h-4 w-4 text-gold-soft" aria-hidden />
          {t.appearance}
        </h2>

        <Toggle
          checked={settings.kurdishDigits}
          onChange={(value) => updateSettings({ kurdishDigits: value })}
          label={t.kurdishDigits}
          hint={t.kurdishDigitsHint}
        />

        <div className="flex items-center justify-between gap-2 border-t border-white/[0.06] pt-2.5">
          <span className="text-[12px] text-slate-300">{t.language}</span>
          <span className="chip chip-neon text-[10px]">{t.languageKurdish}</span>
        </div>

        <div className="border-t border-white/[0.06] pt-2.5">
          <div className="flex items-center justify-between">
            <span className="text-[12px] text-slate-300">{t.defaultCurrency}</span>
            <div className="flex gap-1.5">
              {(['IQD', 'USD'] as Currency[]).map((currency) => (
                <button
                  key={currency}
                  type="button"
                  onClick={() => updateSettings({ defaultCurrency: currency })}
                  className={clsx('pill px-2.5 py-1 text-[10.5px]', settings.defaultCurrency === currency ? 'pill-active' : 'pill-idle')}
                >
                  {currency === 'IQD' ? 'دینار' : 'دۆلار'}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="border-t border-white/[0.06] pt-2.5">
          <label htmlFor="default-fee" className="flex items-center justify-between text-[12px] text-slate-300">
            {t.defaultFee}
            <span className="text-[10px] text-slate-500">{t.defaultFeeHint}</span>
          </label>
          <div className="mt-1.5 flex items-center gap-2">
            <CircleDollarSign className="h-4 w-4 shrink-0 text-gold-soft" aria-hidden />
            <input
              id="default-fee"
              type="number"
              min="0"
              step="250"
              dir="ltr"
              value={settings.defaultFee}
              onChange={(event) => updateSettings({ defaultFee: Math.max(0, Number(event.target.value) || 0) })}
              className="field-input py-2.5 text-left"
            />
          </div>
        </div>
      </section>

      {/* Install */}
      <section className="glass space-y-2.5 p-3.5">
        <h2 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <Smartphone className="h-4 w-4 text-neon-soft" aria-hidden />
          {t.appSection}
        </h2>
        <p className="text-[11px] leading-6 text-slate-400">{t.installAppHint}</p>

        {install.isStandalone ? (
          <div className="flex items-center gap-2 rounded-xl border border-success/25 bg-success/10 px-3 py-2 text-[11.5px] text-success">
            <BadgeCheck className="h-4 w-4" aria-hidden />
            {t.installedAlready}
          </div>
        ) : install.canInstall ? (
          <button type="button" onClick={() => void handleInstall()} className="btn-primary w-full py-2.5 text-[12.5px]">
            <Download className="h-4 w-4" aria-hidden />
            {t.installNow}
          </button>
        ) : (
          <p className="rounded-xl border border-gold/25 bg-gold/10 px-3 py-2 text-[11px] leading-6 text-gold-soft">
            {install.isIos ? t.iosInstallHint : t.installAppHint}
          </p>
        )}

        <div className="flex items-center gap-2 border-t border-white/[0.06] pt-2 text-[10.5px]">
          {online ? (
            <>
              <Wifi className="h-3.5 w-3.5 text-success" aria-hidden />
              <span className="text-slate-400">{t.onlineStatus}</span>
            </>
          ) : (
            <>
              <WifiOff className="h-3.5 w-3.5 text-gold-soft" aria-hidden />
              <span className="text-slate-400">{t.offlineStatus}</span>
            </>
          )}
          <span className="mr-auto flex items-center gap-1 text-slate-500">
            <Database className="h-3.5 w-3.5" aria-hidden />
            {loadingHealth ? (
              <Loader2 className="h-3 w-3 animate-spin" aria-hidden />
            ) : backend === 'postgres' ? (
              t.databasePostgres
            ) : (
              t.databaseFallback
            )}
          </span>
        </div>
      </section>

      {/* Data & sync */}
      <section className="glass space-y-3 p-3.5">
        <h2 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <HardDrive className="h-4 w-4 text-gold-soft" aria-hidden />
          {t.dataSection}
        </h2>

        <div className="grid grid-cols-2 gap-2">
          <div className="glass-inset p-2.5">
            <p className="text-[10px] text-slate-500">{t.orderSingular}</p>
            <p className="text-[15px] font-bold text-white">{deliveries.length}</p>
          </div>
          <div className="glass-inset p-2.5">
            <p className="text-[10px] text-slate-500">{t.cacheSize}</p>
            <p className="text-[15px] font-bold text-white">{formatBytes(cacheSize)}</p>
          </div>
        </div>

        {health && !health.ok && (
          <p className="rounded-xl border border-danger/25 bg-danger/10 px-3 py-2 text-[10.5px] leading-5 text-danger">
            {t.errorRetry}
          </p>
        )}

        <div className="flex items-center justify-between gap-2 rounded-xl border border-white/[0.07] bg-ink-950/40 px-3 py-2">
          <span className="text-[11.5px] text-slate-300">{t.syncSection}</span>
          <div className="flex items-center gap-2">
            <span
              className={clsx(
                'chip text-[10px]',
                outboxCount > 0 ? 'chip-gold' : 'chip-success',
              )}
            >
              {outboxCount > 0 ? `${outboxCount} ${t.pendingSync}` : t.allSynced}
            </span>
            <button
              type="button"
              onClick={() => void flushOutbox()}
              className="grid h-8 w-8 place-items-center rounded-lg border border-white/10 bg-white/[0.05] text-slate-200"
              aria-label={t.syncNow}
              title={t.syncNow}
            >
              <RefreshCw className="h-3.5 w-3.5" aria-hidden />
            </button>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-2">
          <button
            type="button"
            onClick={() => void handleSeed()}
            disabled={busy === 'seed'}
            className="btn-ghost py-2.5 text-[11.5px]"
          >
            {busy === 'seed' ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden />
            ) : (
              <Sparkles className="h-3.5 w-3.5 text-gold-soft" aria-hidden />
            )}
            {t.loadDemo}
          </button>
          <button type="button" onClick={handleClear} className="btn-danger py-2.5 text-[11.5px]">
            <Trash2 className="h-3.5 w-3.5" aria-hidden />
            {t.clearCache}
          </button>
        </div>
      </section>

      {/* About */}
      <section className="glass space-y-2 p-3.5">
        <h2 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <Info className="h-4 w-4 text-neon-soft" aria-hidden />
          {t.aboutSection}
        </h2>
        <p className="text-[11px] leading-6 text-slate-400">{t.aboutText}</p>
        <div className="flex flex-wrap items-center gap-1.5 border-t border-white/[0.06] pt-2">
          <span className="chip text-[10px]">
            {t.version} {APP_VERSION}
          </span>
          <span className="chip chip-success text-[10px]">{t.offlineReady}</span>
          <span className="chip chip-neon text-[10px]">{t.languageKurdish}</span>
          <span className="mr-auto text-[9.5px] text-slate-600">{t.poweredBy}</span>
        </div>
      </section>
    </div>
  );
}
