/**
 * Shared domain types for یوسف دلیڤەری.
 * These types are used by the API layer (server) and the UI (client).
 */

export const DELIVERY_STATUSES = ['pending', 'on_the_way', 'delivered', 'canceled'] as const;
export type DeliveryStatus = (typeof DELIVERY_STATUSES)[number];

export const CURRENCIES = ['IQD', 'USD'] as const;
export type Currency = (typeof CURRENCIES)[number];

export const MESSAGING_CHANNELS = ['whatsapp', 'viber'] as const;
export type MessagingChannel = (typeof MESSAGING_CHANNELS)[number];

export interface Delivery {
  /** order id (uuid) */
  id: string;
  /** sequential, human friendly order number */
  orderNo: number;
  customerId: string;
  name: string;
  /** normalised E.164 phone number, e.g. +9647501234567 */
  primaryPhone: string;
  /** normalised E.164 phone numbers */
  extraPhones: string[];
  latitude: number | null;
  longitude: number | null;
  /** free text address / street */
  address: string;
  /** landmark such as a mosque or shop */
  landmark: string;
  channels: MessagingChannel[];
  customerNotes: string;
  itemNote: string;
  price: number;
  currency: Currency;
  deliveryFee: number;
  status: DeliveryStatus;
  createdAt: string;
  updatedAt: string;
  deliveredAt: string | null;
  /** true while the record only exists in the client outbox */
  pendingSync?: boolean;
}

export interface DeliveryInput {
  customerId?: string;
  name: string;
  primaryPhone: string;
  extraPhones: string[];
  latitude: number | null;
  longitude: number | null;
  address: string;
  landmark: string;
  channels: MessagingChannel[];
  customerNotes: string;
  itemNote: string;
  price: number;
  currency: Currency;
  deliveryFee: number;
  status: DeliveryStatus;
}

export interface DeliveryPatch {
  name?: string;
  primaryPhone?: string;
  extraPhones?: string[];
  latitude?: number | null;
  longitude?: number | null;
  address?: string;
  landmark?: string;
  channels?: MessagingChannel[];
  customerNotes?: string;
  itemNote?: string;
  price?: number;
  currency?: Currency;
  deliveryFee?: number;
  status?: DeliveryStatus;
}

export interface TodayStats {
  deliveredToday: number;
  pendingCount: number;
  onTheWayCount: number;
  canceledToday: number;
  cashToday: number;
  feesToday: number;
  deliveredAllTime: number;
  cashAllTime: number;
  totalCount: number;
}

export interface Customer {
  id: string;
  name: string;
  primaryPhone: string;
  extraPhones: string[];
  latitude: number | null;
  longitude: number | null;
  address: string;
  landmark: string;
  channels: MessagingChannel[];
  notes: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiError {
  error: string;
  message: string;
  fields?: Record<string, string>;
}

export interface ValidationResult<T> {
  ok: boolean;
  data?: T;
  fields?: Record<string, string>;
}

export interface LatLng {
  lat: number;
  lng: number;
}

export type SortMode = 'nearest' | 'newest';
export type StatusFilter = 'all' | 'pending' | 'on_the_way' | 'delivered';
