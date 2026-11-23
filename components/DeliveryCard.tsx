'use client';

import { useState } from 'react';
import Link from 'next/link';
import {
  ChevronLeft,
  CircleDollarSign,
  Copy,
  MapPin,
  MessageCircle,
  Navigation,
  Phone,
  PhoneCall,
  Play,
  CheckCheck,
  Clock,
  Hash,
  MoreHorizontal,
} from 'lucide-react';
import clsx from 'clsx';
import type { Delivery } from '@/lib/types';
import { useStore } from '@/lib/store';
import { useToast } from '@/components/ui/Toast';
import { t } from '@/lib/i18n';
import { distanceTo } from '@/lib/search';
import {
  formatCount,
  formatDistanceSentence,
  formatDuration,
  formatMoney,
  formatTime,
  relativeTime,
} from '@/lib/format';
import { estimateEta } from '@/lib/format';
import { formatPhone, toTelHref, toViberHref, toWhatsAppHref } from '@/lib/phone';
import { StatusBadge } from './StatusBadge';
import { copyText } from '@/lib/clipboard';

interface DeliveryCardProps {
  delivery: Delivery;
  index?: number;
  onShowRoute: (delivery: Delivery) => void;
}

export function DeliveryCard({ delivery, index = 0, onShowRoute }: DeliveryCardProps) {
  const { position, settings, setStatus } = useStore();
  const { toast } = useToast();
  const [expanded, setExpanded] = useState(false);
  const [busy, setBusy] = useState(false);

  const kurdish = settings.kurdishDigits;
  const meters = distanceTo(delivery, position);
  const eta = meters !== null ? estimateEta(meters / 1000) : null;
  const hasCoords = delivery.latitude !== null && delivery.longitude !== null;
  const total = delivery.price + delivery.deliveryFee;
  const etaMessage = t.etaTemplate(delivery.name);

  const changeStatus = async (next: Parameters<typeof setStatus>[1]) => {
    if (busy) return;
    setBusy(true);
    const result = await setStatus(delivery.id, next);
    setBusy(false);
    if (result.ok) {
      toast(result.offline ? t.queuedOffline : t.statusUpdated, result.offline ? 'info' : 'success');
    } else {
      toast(result.message, 'error');
    }
  };

  const copy = async (value: string, message: string) => {
    const ok = await copyText(value);
    toast(ok ? message : t.copyFailed, ok ? 'success' : 'error');
  };

  const addressLine = delivery.address || delivery.landmark || t.noAddress;

  return (
    <article
      className="glass relative overflow-hidden p-3.5 animate-fade-up"
      style={{ animationDelay: `${Math.min(index, 8) * 35}ms` }}
    >
      <span
        className={clsx(
          'absolute inset-y-4 right-0 w-1 rounded-full',
          delivery.status === 'pending' && 'bg-gold',
          delivery.status === 'on_the_way' && 'bg-neon',
          delivery.status === 'delivered' && 'bg-success',
          delivery.status === 'canceled' && 'bg-danger',
        )}
      />

      <div className="flex items-start gap-2 pr-2.5">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <h3 className="truncate text-[15px] font-bold text-white">{delivery.name}</h3>
            <StatusBadge status={delivery.status} short />
          </div>
          <p className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-[10.5px] text-slate-500">
            <span className="inline-flex items-center gap-1">
              <Hash className="h-3 w-3" aria-hidden />
              {formatCount(delivery.orderNo, kurdish)}
            </span>
            <span className="inline-flex items-center gap-1">
              <Clock className="h-3 w-3" aria-hidden />
              {relativeTime(delivery.createdAt, kurdish)}
            </span>
            {delivery.pendingSync && (
              <span className="chip chip-gold text-[9.5px]">{t.pendingSync}</span>
            )}
          </p>
        </div>

        <Link
          href={`/orders/${delivery.id}`}
          aria-label={t.orderDetailsTitle}
          className="grid h-8 w-8 shrink-0 place-items-center rounded-xl border border-white/10 bg-white/[0.04] text-slate-400 transition active:scale-95 hover:text-slate-100"
        >
          <MoreHorizontal className="h-4 w-4" aria-hidden />
        </Link>
      </div>

      {/* Location — tap for the live route sheet */}
      <button
        type="button"
        onClick={() => onShowRoute(delivery)}
        className="mt-2.5 flex w-full items-center gap-2.5 rounded-2xl border border-white/[0.07] bg-ink-950/45 p-2.5 text-right transition active:scale-[0.995] hover:border-neon/25"
      >
        <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-neon/20 bg-neon/10 text-neon-soft">
          <Navigation className="h-4 w-4" aria-hidden />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-[12.5px] font-medium text-slate-100">{addressLine}</span>
          <span className="mt-0.5 flex flex-wrap items-center gap-x-1.5 text-[11px]">
            {meters !== null ? (
              <span className="font-semibold text-neon-soft">{formatDistanceSentence(meters, kurdish)}</span>
            ) : (
              <span className="text-gold-soft">
                {hasCoords ? t.noDriverLocation : t.noLocationForDistance}
              </span>
            )}
            {eta !== null && meters !== null && (
              <span className="text-slate-500">
                · {t.etaLabel} {formatDuration(eta, kurdish)}
              </span>
            )}
          </span>
        </span>
        <ChevronLeft className="h-4 w-4 shrink-0 text-slate-500" aria-hidden />
      </button>

      {/* Primary phone */}
      <div className="mt-2 flex items-center gap-2">
        <a
          href={toTelHref(delivery.primaryPhone)}
          className="flex min-w-0 flex-1 items-center gap-2 rounded-2xl border border-white/10 bg-white/[0.04] px-2.5 py-2 transition active:scale-[0.99] hover:bg-white/[0.07]"
        >
          <Phone className="h-3.5 w-3.5 shrink-0 text-neon-soft" aria-hidden />
          <span dir="ltr" className="min-w-0 flex-1 truncate text-right text-[13px] font-semibold tracking-wide text-slate-100">
            {formatPhone(delivery.primaryPhone)}
          </span>
          <span className="flex shrink-0 items-center gap-1">
            {delivery.channels.includes('whatsapp') && (
              <span className="rounded-md border border-[#25D366]/30 bg-[#25D366]/10 px-1.5 py-0.5 text-[9px] font-bold text-[#3ee27a]">
                WA
              </span>
            )}
            {delivery.channels.includes('viber') && (
              <span className="rounded-md border border-[#7360F2]/35 bg-[#7360F2]/15 px-1.5 py-0.5 text-[9px] font-bold text-[#a99bff]">
                VB
              </span>
            )}
          </span>
        </a>

        {delivery.extraPhones.length > 0 && (
          <button
            type="button"
            onClick={() => setExpanded((value) => !value)}
            aria-expanded={expanded}
            className="chip chip-gold shrink-0 py-2"
          >
            +{formatCount(delivery.extraPhones.length, kurdish)} {t.extraPhonesShort}
          </button>
        )}
      </div>

      {expanded && delivery.extraPhones.length > 0 && (
        <div className="mt-2 space-y-1.5 rounded-2xl border border-white/[0.07] bg-ink-950/40 p-2 animate-fade-up">
          {delivery.extraPhones.map((phone) => (
            <div key={phone} className="flex items-center gap-2">
              <span dir="ltr" className="min-w-0 flex-1 truncate text-right text-[12.5px] font-medium text-slate-200">
                {formatPhone(phone)}
              </span>
              <a
                href={toTelHref(phone)}
                aria-label={t.call}
                className="grid h-8 w-8 place-items-center rounded-lg border border-neon/30 bg-neon/10 text-neon-soft active:scale-95"
              >
                <Phone className="h-3.5 w-3.5" aria-hidden />
              </a>
              <a
                href={toWhatsAppHref(phone)}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={t.whatsapp}
                className="grid h-8 w-8 place-items-center rounded-lg border border-[#25D366]/30 bg-[#25D366]/10 text-[#3ee27a] active:scale-95"
              >
                <MessageCircle className="h-3.5 w-3.5" aria-hidden />
              </a>
              <a
                href={toViberHref(phone)}
                aria-label={t.viber}
                className="grid h-8 w-8 place-items-center rounded-lg border border-[#7360F2]/35 bg-[#7360F2]/15 text-[#a99bff] active:scale-95"
              >
                <PhoneCall className="h-3.5 w-3.5" aria-hidden />
              </a>
            </div>
          ))}
        </div>
      )}

      {/* One-tap channel grid */}
      <div className="mt-2.5 grid grid-cols-4 gap-1.5">
        <a
          href={toTelHref(delivery.primaryPhone)}
          className="flex flex-col items-center gap-1 rounded-xl border border-neon/25 bg-neon/10 py-2 text-[10.5px] font-semibold text-neon-soft transition active:scale-95"
        >
          <Phone className="h-4 w-4" aria-hidden />
          {t.call}
        </a>
        <a
          href={toWhatsAppHref(delivery.primaryPhone)}
          target="_blank"
          rel="noopener noreferrer"
          className="flex flex-col items-center gap-1 rounded-xl border border-[#25D366]/25 bg-[#25D366]/10 py-2 text-[10.5px] font-semibold text-[#3ee27a] transition active:scale-95"
        >
          <MessageCircle className="h-4 w-4" aria-hidden />
          {t.whatsapp}
        </a>
        <a
          href={toViberHref(delivery.primaryPhone)}
          className="flex flex-col items-center gap-1 rounded-xl border border-[#7360F2]/30 bg-[#7360F2]/10 py-2 text-[10.5px] font-semibold text-[#a99bff] transition active:scale-95"
        >
          <PhoneCall className="h-4 w-4" aria-hidden />
          {t.viber}
        </a>
        <button
          type="button"
          onClick={() => onShowRoute(delivery)}
          className="flex flex-col items-center gap-1 rounded-xl border border-white/10 bg-white/[0.05] py-2 text-[10.5px] font-semibold text-slate-200 transition active:scale-95"
        >
          <MapPin className="h-4 w-4" aria-hidden />
          {t.navigate}
        </button>
      </div>

      {/* Footer: money + secondary actions + status CTA */}
      <div className="mt-2.5 flex flex-wrap items-center gap-1.5 border-t border-white/[0.05] pt-2.5">
        <span className="inline-flex items-baseline gap-1">
          <span className="text-[13px] font-bold text-gold-soft">
            {formatMoney(total, delivery.currency, { kurdish })}
          </span>
          <span className="text-[9.5px] text-slate-500">
            ({t.feeLabel} {formatMoney(delivery.deliveryFee, delivery.currency, { kurdish })})
          </span>
        </span>

        <div className="mr-auto flex items-center gap-1.5">
          <a
            href={toWhatsAppHref(delivery.primaryPhone, etaMessage)}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={t.whatsappEta}
            title={t.whatsappEta}
            className="icon-btn-sm h-8 w-8 text-[#3ee27a]"
          >
            <MessageCircle className="h-3.5 w-3.5" aria-hidden />
          </a>
          <button
            type="button"
            onClick={() => copy(addressLine, t.copied)}
            aria-label={t.copyAddress}
            title={t.copyAddress}
            className="icon-btn-sm h-8 w-8"
          >
            <Copy className="h-3.5 w-3.5" aria-hidden />
          </button>
          {delivery.status === 'pending' && (
            <button
              type="button"
              onClick={() => void changeStatus('on_the_way')}
              disabled={busy}
              className="btn bg-gradient-to-br from-gold-soft to-gold-deep px-3 py-1.5 text-[11.5px] text-ink-950 shadow-gold"
            >
              <Play className="h-3.5 w-3.5" aria-hidden />
              {t.markOnTheWayShort}
            </button>
          )}
          {delivery.status === 'on_the_way' && (
            <button
              type="button"
              onClick={() => void changeStatus('delivered')}
              disabled={busy}
              className="btn bg-gradient-to-br from-success to-emerald-600 px-3 py-1.5 text-[11.5px] text-ink-950 shadow-[0_12px_30px_-14px_rgba(52,211,153,0.8)]"
            >
              <CheckCheck className="h-3.5 w-3.5" aria-hidden />
              {t.markDeliveredShort}
            </button>
          )}
          {delivery.status === 'delivered' && delivery.deliveredAt && (
            <span className="chip chip-success text-[10px]">
              <CircleDollarSign className="h-3 w-3" aria-hidden />
              {formatTime(delivery.deliveredAt, kurdish)}
            </span>
          )}
        </div>
      </div>
    </article>
  );
}
