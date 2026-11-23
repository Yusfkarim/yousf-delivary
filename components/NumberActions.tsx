'use client';

import { MessageCircle, Phone, PhoneCall } from 'lucide-react';
import clsx from 'clsx';
import { formatPhone, toTelHref, toViberHref, toWhatsAppHref } from '@/lib/phone';
import { t } from '@/lib/i18n';

interface NumberActionsProps {
  phone: string;
  /** extra phones use the compact layout */
  variant?: 'primary' | 'mini';
  whatsappText?: string;
  showNumber?: boolean;
}

export function NumberActions({
  phone,
  variant = 'primary',
  whatsappText,
  showNumber = true,
}: NumberActionsProps) {
  const size = variant === 'primary' ? 'h-11 w-full' : 'h-9 w-9';
  const icon = variant === 'primary' ? 'h-4 w-4' : 'h-3.5 w-3.5';

  return (
    <div className={clsx('flex items-center gap-1.5', variant === 'primary' && 'w-full')}>
      {showNumber && variant === 'primary' && (
        <span
          dir="ltr"
          className="min-w-0 flex-1 truncate text-right text-[13px] font-semibold tracking-wide text-slate-100"
        >
          {formatPhone(phone)}
        </span>
      )}

      <a
        href={toTelHref(phone)}
        aria-label={`${t.call} ${formatPhone(phone)}`}
        title={t.call}
        className={clsx(
          'grid place-items-center rounded-xl border border-neon/35 bg-neon/10 text-neon-soft transition active:scale-95 hover:bg-neon/20',
          size,
        )}
      >
        <Phone className={icon} aria-hidden />
      </a>

      <a
        href={toWhatsAppHref(phone, whatsappText)}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`${t.whatsapp} ${formatPhone(phone)}`}
        title={t.whatsapp}
        className={clsx(
          'grid place-items-center rounded-xl border border-[#25D366]/35 bg-[#25D366]/10 text-[#3ee27a] transition active:scale-95 hover:bg-[#25D366]/20',
          size,
        )}
      >
        <MessageCircle className={icon} aria-hidden />
      </a>

      <a
        href={toViberHref(phone)}
        aria-label={`${t.viber} ${formatPhone(phone)}`}
        title={t.viber}
        className={clsx(
          'grid place-items-center rounded-xl border border-[#7360F2]/40 bg-[#7360F2]/15 text-[#a99bff] transition active:scale-95 hover:bg-[#7360F2]/25',
          size,
        )}
      >
        <PhoneCall className={icon} aria-hidden />
      </a>
    </div>
  );
}
