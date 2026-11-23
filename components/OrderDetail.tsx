'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ArrowRight,
  Banknote,
  CheckCheck,
  Clock,
  Copy,
  Hash,
  Loader2,
  MapPin,
  MessageCircle,
  Navigation,
  PackageX,
  Pencil,
  PhoneCall,
  Play,
  Trash2,
  TriangleAlert,
} from 'lucide-react';
import clsx from 'clsx';
import { useStore } from '@/lib/store';
import { useToast } from '@/components/ui/Toast';
import { Sheet } from '@/components/ui/Sheet';
import { EmptyState } from '@/components/ui/EmptyState';
import { StatusBadge } from '@/components/StatusBadge';
import { NumberActions } from '@/components/NumberActions';
import { RouteModal } from '@/components/RouteModal';
import { MapView } from '@/components/map/MapView';
import { t, statusMeta } from '@/lib/i18n';
import { formatCount, formatDateTime, formatMoney, formatTime, relativeTime } from '@/lib/format';
import { formatInternational, formatPhone, toWhatsAppHref } from '@/lib/phone';
import { copyText } from '@/lib/clipboard';
import type { DeliveryStatus } from '@/lib/types';

const STEPS: DeliveryStatus[] = ['pending', 'on_the_way', 'delivered'];

export function OrderDetail({ id }: { id: string }) {
  const router = useRouter();
  const { deliveries, loading, settings, position, setStatus, deleteDelivery } = useStore();
  const { toast } = useToast();
  const [showRoute, setShowRoute] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [busy, setBusy] = useState(false);

  const delivery = deliveries.find((item) => item.id === id);
  const kurdish = settings.kurdishDigits;

  if (!delivery) {
    return loading ? (
      <div className="grid place-items-center py-24">
        <Loader2 className="h-6 w-6 animate-spin text-neon" aria-hidden />
        <p className="mt-2 text-[12px] text-slate-400">{t.loading}</p>
      </div>
    ) : (
      <div className="pt-1">
        <EmptyState
          icon={<PackageX className="h-7 w-7" />}
          title={t.notFound}
          body={t.emptyFilteredBody}
          action={
            <Link href="/" className="btn-ghost">
              <ArrowRight className="h-4 w-4" aria-hidden />
              {t.back}
            </Link>
          }
        />
      </div>
    );
  }

  const hasCoords = delivery.latitude !== null && delivery.longitude !== null;
  const addressLine = [delivery.address, delivery.landmark].filter(Boolean).join(' — ');
  const total = delivery.price + delivery.deliveryFee;
  const stepIndex = STEPS.indexOf(delivery.status);

  const copy = async (value: string, message: string) => {
    if (!value) return;
    const ok = await copyText(value);
    toast(ok ? message : t.copyFailed, ok ? 'success' : 'error');
  };

  const changeStatus = async (next: DeliveryStatus) => {
    if (busy) return;
    setBusy(true);
    const result = await setStatus(delivery.id, next);
    setBusy(false);
    toast(result.ok ? (result.offline ? t.queuedOffline : t.statusUpdated) : result.message, result.ok ? 'success' : 'error');
  };

  const remove = async () => {
    setBusy(true);
    const result = await deleteDelivery(delivery.id);
    setBusy(false);
    setConfirmDelete(false);
    if (result.ok) {
      toast(result.offline ? t.queuedOffline : t.orderDeleted, result.offline ? 'info' : 'success');
      router.push('/');
    } else {
      toast(result.message, 'error');
    }
  };

  return (
    <div className="space-y-3.5 pt-1">
      <div className="flex items-center gap-2">
        <button type="button" onClick={() => router.back()} className="icon-btn-sm" aria-label={t.back}>
          <ArrowRight className="h-4 w-4" aria-hidden />
        </button>
        <h1 className="text-[15px] font-bold text-white">{t.orderDetailsTitle}</h1>
        <span className="mr-auto inline-flex items-center gap-1 text-[10.5px] text-slate-500">
          <Hash className="h-3 w-3" aria-hidden />
          {formatCount(delivery.orderNo, kurdish)}
        </span>
      </div>

      {/* Identity + progress */}
      <section className="glass p-3.5">
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="truncate text-[16px] font-bold text-white">{delivery.name}</h2>
              <StatusBadge status={delivery.status} />
            </div>
            <p className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-[10.5px] text-slate-500">
              <span className="inline-flex items-center gap-1">
                <Clock className="h-3 w-3" aria-hidden />
                {formatDateTime(delivery.createdAt, kurdish)}
              </span>
              <span>· {relativeTime(delivery.createdAt, kurdish)}</span>
            </p>
            {delivery.pendingSync && <span className="chip chip-gold mt-1.5 text-[10px]">{t.pendingSync}</span>}
          </div>
          <Link href={`/orders/${delivery.id}/edit`} className="icon-btn-sm" aria-label={t.edit}>
            <Pencil className="h-3.5 w-3.5" aria-hidden />
          </Link>
        </div>

        {delivery.status !== 'canceled' && (
          <div className="mt-3 flex items-center gap-1">
            {STEPS.map((step, index) => {
              const done = stepIndex >= index;
              return (
                <div key={step} className="flex flex-1 items-center gap-1">
                  <span
                    className={clsx(
                      'grid h-6 w-6 shrink-0 place-items-center rounded-full border text-[10px] font-bold transition',
                      done
                        ? step === 'delivered'
                          ? 'border-success/50 bg-success/20 text-success'
                          : 'border-neon/50 bg-neon/20 text-neon-soft'
                        : 'border-white/10 bg-white/[0.03] text-slate-600',
                    )}
                  >
                    {formatCount(index + 1, kurdish)}
                  </span>
                  {index < STEPS.length - 1 && (
                    <span
                      className={clsx(
                        'h-0.5 flex-1 rounded-full',
                        stepIndex > index ? 'bg-neon/60' : 'bg-white/10',
                      )}
                    />
                  )}
                </div>
              );
            })}
          </div>
        )}
        {delivery.status !== 'canceled' && (
          <div className="mt-1 flex justify-between text-[9.5px] text-slate-500">
            {STEPS.map((step) => (
              <span key={step}>{statusMeta[step].shortLabel}</span>
            ))}
          </div>
        )}
        {delivery.status === 'canceled' && (
          <p className="mt-3 rounded-xl border border-danger/30 bg-danger/10 px-3 py-2 text-[11.5px] text-danger">
            {statusMeta.canceled.description}
          </p>
        )}
      </section>

      {/* Contact */}
      <section className="glass space-y-2.5 p-3.5">
        <h3 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <PhoneCall className="h-4 w-4 text-neon-soft" aria-hidden />
          {t.contactSection}
        </h3>

        <div className="flex items-center gap-2 rounded-2xl border border-white/[0.07] bg-ink-950/40 p-2.5">
          <div className="min-w-0 flex-1">
            <p className="text-[10px] text-slate-500">{t.primaryPhone}</p>
            <p dir="ltr" className="truncate text-right text-[13.5px] font-semibold tracking-wide text-slate-100">
              {formatPhone(delivery.primaryPhone)}
            </p>
            <p dir="ltr" className="truncate text-right text-[9.5px] text-slate-600">
              {formatInternational(delivery.primaryPhone)}
            </p>
          </div>
          <div className="flex shrink-0 items-center gap-1">
            {delivery.channels.includes('whatsapp') && <span className="chip text-[9px] text-[#3ee27a]">WA</span>}
            {delivery.channels.includes('viber') && <span className="chip text-[9px] text-[#a99bff]">VB</span>}
          </div>
        </div>

        <NumberActions phone={delivery.primaryPhone} whatsappText={t.etaTemplate(delivery.name)} showNumber={false} />

        {delivery.extraPhones.length > 0 && (
          <div className="space-y-1.5 rounded-2xl border border-white/[0.07] bg-ink-950/40 p-2.5">
            <p className="text-[10px] text-slate-500">
              {t.extraPhonesTitle} · {formatCount(delivery.extraPhones.length, kurdish)}
            </p>
            {delivery.extraPhones.map((phone) => (
              <div key={phone} className="flex items-center gap-2">
                <span dir="ltr" className="min-w-0 flex-1 truncate text-right text-[12.5px] text-slate-200">
                  {formatPhone(phone)}
                </span>
                <NumberActions phone={phone} variant="mini" showNumber={false} />
              </div>
            ))}
          </div>
        )}

        <div className="grid grid-cols-2 gap-1.5">
          <a
            href={toWhatsAppHref(delivery.primaryPhone, t.etaTemplate(delivery.name))}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-1.5 rounded-xl border border-[#25D366]/25 bg-[#25D366]/10 py-2 text-[11px] font-semibold text-[#3ee27a]"
          >
            <MessageCircle className="h-3.5 w-3.5" aria-hidden />
            {t.whatsappEta}
          </a>
          <a
            href={toWhatsAppHref(delivery.primaryPhone, t.deliveredTemplate(delivery.name))}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-1.5 rounded-xl border border-[#25D366]/25 bg-[#25D366]/10 py-2 text-[11px] font-semibold text-[#3ee27a]"
          >
            <CheckCheck className="h-3.5 w-3.5" aria-hidden />
            {t.statusDelivered}
          </a>
        </div>
      </section>

      {/* Location */}
      <section className="glass overflow-hidden">
        <div className="relative h-40">
          <MapView
            markers={
              hasCoords
                ? [
                    {
                      id: delivery.id,
                      position: { lat: delivery.latitude as number, lng: delivery.longitude as number },
                      label: delivery.name,
                      kind: 'customer',
                      status: delivery.status,
                    },
                  ]
                : []
            }
            me={position}
            center={
              hasCoords
                ? { lat: delivery.latitude as number, lng: delivery.longitude as number }
                : undefined
            }
            zoom={15}
            fitToMarkers={hasCoords}
            interactive={false}
            showZoomControls={false}
          />
          <button
            type="button"
            onClick={() => setShowRoute(true)}
            className="absolute inset-x-2 bottom-2 flex items-center justify-center gap-1.5 rounded-xl border border-white/15 bg-ink-950/80 py-2 text-[11px] font-semibold text-neon-soft backdrop-blur-md active:scale-[0.99]"
          >
            <Navigation className="h-3.5 w-3.5" aria-hidden />
            {t.startNavigation}
          </button>
        </div>

        <div className="space-y-2 p-3.5">
          <div className="flex items-start gap-2">
            <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-gold-soft" aria-hidden />
            <div className="min-w-0 flex-1">
              <p className="text-[12.5px] leading-6 text-slate-100">{addressLine || t.noAddress}</p>
              {hasCoords && (
                <p dir="ltr" className="mt-0.5 text-[10px] text-slate-500">
                  {(delivery.latitude as number).toFixed(6)}, {(delivery.longitude as number).toFixed(6)}
                </p>
              )}
            </div>
            <div className="flex shrink-0 gap-1">
              <button
                type="button"
                onClick={() => void copy(addressLine, t.copied)}
                className="icon-btn-sm h-8 w-8"
                aria-label={t.copyAddress}
                title={t.copyAddress}
              >
                <Copy className="h-3.5 w-3.5" aria-hidden />
              </button>
              {hasCoords && (
                <button
                  type="button"
                  onClick={() =>
                    void copy(
                      `${delivery.latitude?.toFixed(6)}, ${delivery.longitude?.toFixed(6)}`,
                      t.copyCoords,
                    )
                  }
                  className="icon-btn-sm h-8 w-8"
                  aria-label={t.copyCoords}
                  title={t.copyCoords}
                >
                  <Navigation className="h-3.5 w-3.5" aria-hidden />
                </button>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Order details */}
      <section className="glass space-y-2.5 p-3.5">
        <h3 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <Banknote className="h-4 w-4 text-gold-soft" aria-hidden />
          {t.orderSection}
        </h3>

        <p className="rounded-xl border border-white/[0.07] bg-ink-950/40 px-3 py-2 text-[12px] leading-6 text-slate-200">
          {delivery.itemNote || t.noItem}
        </p>

        <dl className="space-y-1.5 text-[12px]">
          <div className="flex items-center justify-between">
            <dt className="text-slate-400">{t.priceLabel}</dt>
            <dd className="font-semibold text-slate-100">
              {formatMoney(delivery.price, delivery.currency, { kurdish })}
            </dd>
          </div>
          <div className="flex items-center justify-between">
            <dt className="text-slate-400">{t.feeLabel}</dt>
            <dd className="font-semibold text-slate-100">
              {formatMoney(delivery.deliveryFee, delivery.currency, { kurdish })}
            </dd>
          </div>
          <div className="flex items-center justify-between border-t border-white/[0.06] pt-1.5">
            <dt className="text-slate-300">{t.totalLabel}</dt>
            <dd className="text-[14px] font-bold text-gold-soft">
              {formatMoney(total, delivery.currency, { kurdish })}
            </dd>
          </div>
        </dl>

        {delivery.customerNotes && (
          <p className="rounded-xl border border-gold/20 bg-gold/[0.07] px-3 py-2 text-[11.5px] leading-6 text-gold-soft">
            {delivery.customerNotes}
          </p>
        )}

        <div className="flex flex-wrap items-center gap-1.5 border-t border-white/[0.06] pt-2 text-[10px] text-slate-500">
          <span>{t.createdAtLabel}: {formatDateTime(delivery.createdAt, kurdish)}</span>
          {delivery.deliveredAt && (
            <span className="chip chip-success text-[9.5px]">
              {t.deliveredAtLabel}: {formatTime(delivery.deliveredAt, kurdish)}
            </span>
          )}
        </div>
      </section>

      {/* Status actions */}
      <section className="grid grid-cols-1 gap-2 sm:grid-cols-3">
        {delivery.status !== 'on_the_way' && delivery.status !== 'delivered' && (
          <button
            type="button"
            onClick={() => void changeStatus('on_the_way')}
            disabled={busy}
            className="btn-gold py-2.5 text-[12.5px]"
          >
            <Play className="h-4 w-4" aria-hidden />
            {t.markOnTheWay}
          </button>
        )}
        {delivery.status !== 'delivered' && (
          <button
            type="button"
            onClick={() => void changeStatus('delivered')}
            disabled={busy}
            className="btn bg-gradient-to-br from-success to-emerald-600 py-2.5 text-[12.5px] text-ink-950"
          >
            <CheckCheck className="h-4 w-4" aria-hidden />
            {t.markDelivered}
          </button>
        )}
        {delivery.status !== 'canceled' && delivery.status !== 'delivered' && (
          <button
            type="button"
            onClick={() => void changeStatus('canceled')}
            disabled={busy}
            className="btn-danger py-2.5 text-[12.5px]"
          >
            <TriangleAlert className="h-4 w-4" aria-hidden />
            {t.markCanceled}
          </button>
        )}
        {delivery.status === 'delivered' && (
          <button
            type="button"
            onClick={() => void changeStatus('pending')}
            disabled={busy}
            className="btn-ghost py-2.5 text-[12.5px]"
          >
            <Clock className="h-4 w-4" aria-hidden />
            {t.statusPending}
          </button>
        )}
      </section>

      <button
        type="button"
        onClick={() => setConfirmDelete(true)}
        className="mx-auto flex items-center gap-1.5 pb-2 text-[11px] text-danger/80 underline decoration-dotted"
      >
        <Trash2 className="h-3 w-3" aria-hidden />
        {t.deleteOrderTitle}
      </button>

      <RouteModal delivery={showRoute ? delivery : null} onClose={() => setShowRoute(false)} />

      <Sheet
        open={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        title={t.deleteOrderTitle}
        footer={
          <div className="flex gap-2">
            <button type="button" onClick={() => setConfirmDelete(false)} className="btn-ghost flex-1 py-2.5 text-[12.5px]">
              {t.cancel}
            </button>
            <button
              type="button"
              onClick={() => void remove()}
              disabled={busy}
              className="btn bg-danger py-2.5 text-[12.5px] font-semibold text-white"
            >
              <Trash2 className="h-4 w-4" aria-hidden />
              {t.delete}
            </button>
          </div>
        }
      >
        <p className="text-[12.5px] leading-7 text-slate-300">{t.deleteOrderBody}</p>
      </Sheet>
    </div>
  );
}
