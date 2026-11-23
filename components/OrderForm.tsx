'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  AlertCircle,
  Banknote,
  ChevronLeft,
  Loader2,
  MapPin,
  MessageCircle,
  Phone,
  PhoneCall,
  Plus,
  Save,
  Trash2,
  User,
  X,
} from 'lucide-react';
import clsx from 'clsx';
import { useStore } from '@/lib/store';
import { useToast } from '@/components/ui/Toast';
import { MapPicker } from '@/components/MapPicker';
import { t, statusMeta } from '@/lib/i18n';
import { DELIVERY_STATUSES, type Currency, type Delivery, type LatLng, type MessagingChannel } from '@/lib/types';
import { normalizePhone, formatPhone } from '@/lib/phone';
import { validateDeliveryInput } from '@/lib/validation';
import { readCache, removeCache, writeCache } from '@/lib/cache';
import { formatMoney } from '@/lib/format';

interface DraftState {
  name: string;
  primaryPhone: string;
  extraPhones: string[];
  coords: LatLng | null;
  address: string;
  landmark: string;
  channels: MessagingChannel[];
  customerNotes: string;
  itemNote: string;
  price: string;
  deliveryFee: string;
  currency: Currency;
  status: (typeof DELIVERY_STATUSES)[number];
}

const DRAFT_KEY = 'yd.draft.v1';

const emptyDraft: DraftState = {
  name: '',
  primaryPhone: '',
  extraPhones: [],
  coords: null,
  address: '',
  landmark: '',
  channels: ['whatsapp'],
  customerNotes: '',
  itemNote: '',
  price: '',
  deliveryFee: '',
  currency: 'IQD',
  status: 'pending',
};

function draftFromDelivery(delivery: Delivery): DraftState {
  return {
    name: delivery.name,
    primaryPhone: delivery.primaryPhone,
    extraPhones: [...delivery.extraPhones],
    coords:
      delivery.latitude !== null && delivery.longitude !== null
        ? { lat: delivery.latitude, lng: delivery.longitude }
        : null,
    address: delivery.address,
    landmark: delivery.landmark,
    channels: [...delivery.channels],
    customerNotes: delivery.customerNotes,
    itemNote: delivery.itemNote,
    price: delivery.price ? String(delivery.price) : '',
    deliveryFee: delivery.deliveryFee ? String(delivery.deliveryFee) : '',
    currency: delivery.currency,
    status: delivery.status,
  };
}

interface OrderFormProps {
  mode: 'create' | 'edit';
  delivery?: Delivery;
}

export function OrderForm({ mode, delivery }: OrderFormProps) {
  const router = useRouter();
  const { createDelivery, updateDelivery, settings, position } = useStore();
  const { toast } = useToast();

  const [draft, setDraft] = useState<DraftState>(() =>
    delivery ? draftFromDelivery(delivery) : emptyDraft,
  );
  const [fields, setFields] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const hydrated = useRef(false);

  // Restore a locally saved draft (new orders only).
  useEffect(() => {
    if (mode !== 'create') return;
    if (hydrated.current) return;
    hydrated.current = true;

    const saved = readCache<Partial<DraftState> | null>(DRAFT_KEY, null);
    setDraft((prev) => {
      const next = { ...prev };
      if (saved && typeof saved === 'object') {
        next.name = typeof saved.name === 'string' ? saved.name : next.name;
        next.primaryPhone = typeof saved.primaryPhone === 'string' ? saved.primaryPhone : next.primaryPhone;
        next.extraPhones = Array.isArray(saved.extraPhones) ? saved.extraPhones.filter((item) => typeof item === 'string') : [];
        next.coords =
          saved.coords &&
          typeof saved.coords === 'object' &&
          typeof (saved.coords as LatLng).lat === 'number' &&
          typeof (saved.coords as LatLng).lng === 'number'
            ? (saved.coords as LatLng)
            : null;
        next.address = typeof saved.address === 'string' ? saved.address : '';
        next.landmark = typeof saved.landmark === 'string' ? saved.landmark : '';
        next.channels = Array.isArray(saved.channels) ? saved.channels : ['whatsapp'];
        next.customerNotes = typeof saved.customerNotes === 'string' ? saved.customerNotes : '';
        next.itemNote = typeof saved.itemNote === 'string' ? saved.itemNote : '';
        next.price = typeof saved.price === 'string' ? saved.price : '';
        next.deliveryFee =
          typeof saved.deliveryFee === 'string' && saved.deliveryFee
            ? saved.deliveryFee
            : String(settings.defaultFee);
        next.currency = saved.currency === 'USD' ? 'USD' : settings.defaultCurrency;
        next.status = typeof saved.status === 'string' ? (saved.status as DraftState['status']) : 'pending';
        return next;
      }
      return {
        ...next,
        currency: settings.defaultCurrency,
        deliveryFee: String(settings.defaultFee),
      };
    });
  }, [mode, settings.defaultCurrency, settings.defaultFee]);

  // Persist the draft on every edit (new orders only).
  useEffect(() => {
    if (mode !== 'create' || !hydrated.current) return;
    const hasContent =
      draft.name || draft.primaryPhone || draft.address || draft.itemNote || draft.extraPhones.length > 0;
    if (!hasContent) return;
    writeCache(DRAFT_KEY, draft);
  }, [draft, mode]);

  const update = <K extends keyof DraftState>(key: K, value: DraftState[K]) => {
    setDraft((prev) => ({ ...prev, [key]: value }));
    setFields((prev) => {
      if (!prev[key as string]) return prev;
      const next = { ...prev };
      delete next[key as string];
      return next;
    });
  };

  const phonePreview = useMemo(() => normalizePhone(draft.primaryPhone), [draft.primaryPhone]);

  const extraPhoneErrors = useMemo(
    () =>
      draft.extraPhones.map((phone) => {
        if (!phone.trim()) return null;
        return normalizePhone(phone) ? null : t.validationExtraPhone;
      }),
    [draft.extraPhones],
  );

  const toggleChannel = (channel: MessagingChannel) => {
    update(
      'channels',
      draft.channels.includes(channel)
        ? draft.channels.filter((item) => item !== channel)
        : [...draft.channels, channel],
    );
  };

  const total = Number(draft.price || 0) + Number(draft.deliveryFee || 0);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (saving) return;

    const payload = {
      name: draft.name,
      primaryPhone: draft.primaryPhone,
      extraPhones: draft.extraPhones.filter((phone) => phone.trim()),
      latitude: draft.coords?.lat ?? null,
      longitude: draft.coords?.lng ?? null,
      address: draft.address,
      landmark: draft.landmark,
      channels: draft.channels,
      customerNotes: draft.customerNotes,
      itemNote: draft.itemNote,
      price: Number(draft.price || 0),
      currency: draft.currency,
      deliveryFee: Number(draft.deliveryFee || 0),
      status: draft.status,
      customerId: delivery?.customerId,
    };

    const validation = validateDeliveryInput(payload);
    if (!validation.ok || !validation.data) {
      setFields(validation.fields ?? {});
      toast(Object.values(validation.fields ?? {})[0] ?? t.errorRetry, 'error');
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    setSaving(true);
    const result =
      mode === 'edit' && delivery
        ? await updateDelivery(delivery.id, validation.data)
        : await createDelivery(validation.data);
    setSaving(false);

    if (!result.ok) {
      setFields(result.fields ?? {});
      toast(result.message, 'error');
      return;
    }

    if (mode === 'create') removeCache(DRAFT_KEY);
    toast(
      result.offline
        ? t.queuedOffline
        : mode === 'edit'
          ? t.orderUpdated
          : t.orderSaved,
      result.offline ? 'info' : 'success',
    );

    if (mode === 'create' && result.delivery) router.push(`/orders/${result.delivery.id}`);
    else router.push('/');
  };

  const error = (key: string) =>
    fields[key] ? (
      <p className="field-error">
        <AlertCircle className="h-3 w-3" aria-hidden />
        {fields[key]}
      </p>
    ) : null;

  return (
    <form onSubmit={handleSubmit} className="space-y-3.5 pt-1" noValidate>
      <div className="flex items-center gap-2 px-1">
        <h1 className="text-lg font-bold text-white">{mode === 'edit' ? t.editOrder : t.newOrder}</h1>
      </div>

      {Object.keys(fields).length > 0 && (
        <div className="rounded-2xl border border-danger/30 bg-danger/10 px-3.5 py-2.5 text-[12px] text-danger">
          <div className="flex items-center gap-2 font-semibold">
            <AlertCircle className="h-4 w-4" aria-hidden />
            {t.error}
          </div>
          <ul className="mt-1 space-y-0.5 pr-6">
            {Object.entries(fields).map(([key, message]) => (
              <li key={key} className="list-disc">
                {message}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Customer */}
      <section className="glass space-y-3 p-3.5">
        <h2 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <User className="h-4 w-4 text-neon-soft" aria-hidden />
          {t.customerSection}
        </h2>

        <div>
          <label htmlFor="customer-name" className="field-label">
            {t.customerName} <span className="text-danger">*</span>
          </label>
          <input
            id="customer-name"
            value={draft.name}
            onChange={(event) => update('name', event.target.value)}
            placeholder={t.customerNamePlaceholder}
            autoComplete="off"
            className={clsx('field-input', fields.name && 'field-input-error')}
          />
          {error('name')}
        </div>

        <div>
          <label htmlFor="primary-phone" className="field-label">
            {t.primaryPhone} <span className="text-danger">*</span>
          </label>
          <input
            id="primary-phone"
            type="tel"
            inputMode="tel"
            dir="ltr"
            value={draft.primaryPhone}
            onChange={(event) => update('primaryPhone', event.target.value)}
            placeholder={t.primaryPhonePlaceholder}
            autoComplete="off"
            className={clsx('field-input text-left tracking-wide', fields.primaryPhone && 'field-input-error')}
          />
          {phonePreview && (
            <p className="mt-1 px-1 text-[10.5px] text-slate-500" dir="ltr">
              {formatPhone(phonePreview)} · {phonePreview}
            </p>
          )}
          {error('primaryPhone')}
        </div>

        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="field-label mb-0">{t.extraPhonesTitle}</span>
            <button
              type="button"
              onClick={() => update('extraPhones', [...draft.extraPhones, ''])}
              disabled={draft.extraPhones.length >= 5}
              className="inline-flex items-center gap-1 rounded-lg border border-neon/25 bg-neon/10 px-2 py-1 text-[10.5px] font-semibold text-neon-soft disabled:opacity-40"
            >
              <Plus className="h-3 w-3" aria-hidden />
              {t.addExtraPhone}
            </button>
          </div>

          {draft.extraPhones.map((phone, index) => (
            <div key={index} className="space-y-1">
              <div className="flex items-center gap-1.5">
                <input
                  type="tel"
                  inputMode="tel"
                  dir="ltr"
                  value={phone}
                  onChange={(event) => {
                    const next = [...draft.extraPhones];
                    next[index] = event.target.value;
                    update('extraPhones', next);
                  }}
                  placeholder={t.extraPhonePlaceholder}
                  className={clsx(
                    'field-input flex-1 py-2.5 text-left text-[14px] tracking-wide',
                    extraPhoneErrors[index] && 'field-input-error',
                  )}
                />
                <button
                  type="button"
                  onClick={() =>
                    update(
                      'extraPhones',
                      draft.extraPhones.filter((_, itemIndex) => itemIndex !== index),
                    )
                  }
                  aria-label={t.removePhone}
                  className="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-danger/30 bg-danger/10 text-danger active:scale-95"
                >
                  <X className="h-4 w-4" aria-hidden />
                </button>
              </div>
              {extraPhoneErrors[index] && (
                <p className="field-error">{extraPhoneErrors[index]}</p>
              )}
            </div>
          ))}
          {error('extraPhones')}
        </div>

        <div>
          <span className="field-label">{t.contactSection}</span>
          <div className="flex gap-1.5">
            <button
              type="button"
              onClick={() => toggleChannel('whatsapp')}
              aria-pressed={draft.channels.includes('whatsapp')}
              className={clsx(
                'flex flex-1 items-center justify-center gap-1.5 rounded-xl border py-2 text-[11.5px] font-semibold transition',
                draft.channels.includes('whatsapp')
                  ? 'border-[#25D366]/50 bg-[#25D366]/15 text-[#3ee27a]'
                  : 'border-white/10 bg-white/[0.03] text-slate-400',
              )}
            >
              <MessageCircle className="h-3.5 w-3.5" aria-hidden />
              {t.whatsapp}
              {draft.channels.includes('whatsapp') && ' ✓'}
            </button>
            <button
              type="button"
              onClick={() => toggleChannel('viber')}
              aria-pressed={draft.channels.includes('viber')}
              className={clsx(
                'flex flex-1 items-center justify-center gap-1.5 rounded-xl border py-2 text-[11.5px] font-semibold transition',
                draft.channels.includes('viber')
                  ? 'border-[#7360F2]/50 bg-[#7360F2]/20 text-[#a99bff]'
                  : 'border-white/10 bg-white/[0.03] text-slate-400',
              )}
            >
              <PhoneCall className="h-3.5 w-3.5" aria-hidden />
              {t.viber}
              {draft.channels.includes('viber') && ' ✓'}
            </button>
          </div>
        </div>
      </section>

      {/* Location */}
      <section className="glass space-y-3 p-3.5">
        <h2 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <MapPin className="h-4 w-4 text-gold-soft" aria-hidden />
          {t.locationSection}
        </h2>

        <MapPicker value={draft.coords} onChange={(value) => update('coords', value)} error={fields.latitude} />

        <div>
          <label htmlFor="address" className="field-label">
            {t.addressLabel}
          </label>
          <input
            id="address"
            value={draft.address}
            onChange={(event) => update('address', event.target.value)}
            placeholder={t.addressPlaceholder}
            className="field-input"
          />
        </div>

        <div>
          <label htmlFor="landmark" className="field-label">
            {t.landmarkLabel}
          </label>
          <input
            id="landmark"
            value={draft.landmark}
            onChange={(event) => update('landmark', event.target.value)}
            placeholder={t.landmarkPlaceholder}
            className="field-input"
          />
        </div>

        {!position && (
          <p className="px-1 text-[10.5px] text-gold-soft">{t.noDriverLocation}</p>
        )}
      </section>

      {/* Order */}
      <section className="glass space-y-3 p-3.5">
        <h2 className="flex items-center gap-2 text-[12.5px] font-semibold text-slate-200">
          <Banknote className="h-4 w-4 text-gold-soft" aria-hidden />
          {t.orderSection}
        </h2>

        <div>
          <label htmlFor="item-note" className="field-label">
            {t.itemNoteLabel}
          </label>
          <textarea
            id="item-note"
            value={draft.itemNote}
            onChange={(event) => update('itemNote', event.target.value)}
            placeholder={t.itemNotePlaceholder}
            rows={2}
            className="field-input resize-none leading-6"
          />
        </div>

        <div className="grid grid-cols-2 gap-2">
          <div>
            <label htmlFor="price" className="field-label">
              {t.priceLabelShort}
            </label>
            <input
              id="price"
              type="number"
              inputMode="decimal"
              min="0"
              step="250"
              dir="ltr"
              value={draft.price}
              onChange={(event) => update('price', event.target.value)}
              placeholder="0"
              className={clsx('field-input text-left', fields.price && 'field-input-error')}
            />
            {error('price')}
          </div>
          <div>
            <label htmlFor="fee" className="field-label">
              {t.feeLabelShort}
            </label>
            <input
              id="fee"
              type="number"
              inputMode="decimal"
              min="0"
              step="250"
              dir="ltr"
              value={draft.deliveryFee}
              onChange={(event) => update('deliveryFee', event.target.value)}
              placeholder="0"
              className={clsx('field-input text-left', fields.deliveryFee && 'field-input-error')}
            />
            {error('deliveryFee')}
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex gap-1.5">
            {(['IQD', 'USD'] as Currency[]).map((currency) => (
              <button
                key={currency}
                type="button"
                onClick={() => update('currency', currency)}
                aria-pressed={draft.currency === currency}
                className={clsx('pill px-3 py-1.5 text-[11px]', draft.currency === currency ? 'pill-active' : 'pill-idle')}
              >
                {currency === 'IQD' ? 'دیناری عێراقی' : 'دۆلار'}
              </button>
            ))}
          </div>
          <span className="mr-auto text-[11px] text-slate-400">
            {t.totalLabel}:{' '}
            <span className="font-bold text-gold-soft">
              {formatMoney(total, draft.currency, { kurdish: settings.kurdishDigits })}
            </span>
          </span>
        </div>

        <div>
          <span className="field-label">{t.statusLabel}</span>
          <div className="grid grid-cols-2 gap-1.5">
            {DELIVERY_STATUSES.map((status) => (
              <button
                key={status}
                type="button"
                onClick={() => update('status', status)}
                aria-pressed={draft.status === status}
                className={clsx(
                  'flex items-center justify-center gap-1.5 rounded-xl border py-2 text-[11.5px] font-semibold transition',
                  draft.status === status
                    ? `${statusMeta[status].chip} border`
                    : 'border-white/10 bg-white/[0.03] text-slate-400',
                )}
              >
                <span className={clsx('h-1.5 w-1.5 rounded-full', statusMeta[status].dot)} />
                {statusMeta[status].label}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label htmlFor="customer-notes" className="field-label">
            {t.customerNotesLabel}
          </label>
          <textarea
            id="customer-notes"
            value={draft.customerNotes}
            onChange={(event) => update('customerNotes', event.target.value)}
            placeholder={t.customerNotesPlaceholder}
            rows={2}
            className="field-input resize-none leading-6"
          />
        </div>
      </section>

      <div className="glass sticky bottom-[calc(var(--nav-height)+0.75rem)] z-30 flex items-center gap-2 p-2.5">
        <button
          type="button"
          onClick={() => router.back()}
          className="btn-ghost px-3 py-2.5 text-[12px]"
        >
          <ChevronLeft className="h-4 w-4 rotate-180" aria-hidden />
          {t.cancel}
        </button>
        <button type="submit" disabled={saving} className="btn-primary flex-1 py-2.5 text-[13px]">
          {saving ? (
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
          ) : (
            <Save className="h-4 w-4" aria-hidden />
          )}
          {saving ? t.saving : mode === 'edit' ? t.saveChanges : t.saveOrder}
        </button>
      </div>

      {mode === 'create' && (
        <button
          type="button"
          onClick={() => {
            removeCache(DRAFT_KEY);
            setDraft({ ...emptyDraft, deliveryFee: String(settings.defaultFee), currency: settings.defaultCurrency });
            setFields({});
            toast(t.cacheCleared, 'info');
          }}
          className="mx-auto flex items-center gap-1.5 pb-2 text-[10.5px] text-slate-500 underline decoration-dotted"
        >
          <Trash2 className="h-3 w-3" aria-hidden />
          {t.clearDraft}
        </button>
      )}
    </form>
  );
}
