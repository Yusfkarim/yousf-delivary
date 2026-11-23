import type { Delivery } from './types';

export interface PeriodSummary {
  delivered: number;
  onTheWay: number;
  pending: number;
  canceled: number;
  orders: number;
  /** price + delivery fee of delivered orders */
  cash: number;
  /** delivery fees only */
  fees: number;
  /** average delivery fee */
  averageFee: number;
}

function isAfter(value: string | null, since: Date | null): boolean {
  if (!since) return true;
  if (!value) return false;
  const time = new Date(value).getTime();
  return Number.isFinite(time) && time >= since.getTime();
}

export function summarize(deliveries: Delivery[], since: Date | null = null): PeriodSummary {
  let delivered = 0;
  let onTheWay = 0;
  let pending = 0;
  let canceled = 0;
  let cash = 0;
  let fees = 0;

  for (const delivery of deliveries) {
    if (delivery.status === 'delivered') {
      if (isAfter(delivery.deliveredAt ?? delivery.createdAt, since)) {
        delivered += 1;
        cash += delivery.price + delivery.deliveryFee;
        fees += delivery.deliveryFee;
      }
      continue;
    }

    if (!isAfter(delivery.createdAt, since)) continue;

    if (delivery.status === 'pending') pending += 1;
    else if (delivery.status === 'on_the_way') onTheWay += 1;
    else if (delivery.status === 'canceled') canceled += 1;
  }

  return {
    delivered,
    onTheWay,
    pending,
    canceled,
    orders: delivered + onTheWay + pending + canceled,
    cash,
    fees,
    averageFee: delivered > 0 ? Math.round(fees / delivered) : 0,
  };
}

export function startOfDay(date: Date = new Date()): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function daysAgo(days: number, from: Date = new Date()): Date {
  const date = startOfDay(from);
  date.setDate(date.getDate() - days);
  return date;
}

export interface DailyPoint {
  date: Date;
  label: string;
  count: number;
  cash: number;
}

/** Deliveries per day for the last `days` days, oldest first. */
export function dailySeries(deliveries: Delivery[], days = 7): DailyPoint[] {
  const points: DailyPoint[] = [];
  const today = startOfDay();

  for (let offset = days - 1; offset >= 0; offset -= 1) {
    const day = new Date(today);
    day.setDate(today.getDate() - offset);
    const next = new Date(day);
    next.setDate(day.getDate() + 1);

    let count = 0;
    let cash = 0;
    for (const delivery of deliveries) {
      if (delivery.status !== 'delivered') continue;
      const when = new Date(delivery.deliveredAt ?? delivery.createdAt);
      if (when >= day && when < next) {
        count += 1;
        cash += delivery.price + delivery.deliveryFee;
      }
    }

    points.push({
      date: day,
      label: day.toLocaleDateString('en-GB', { weekday: 'short' }),
      count,
      cash,
    });
  }

  return points;
}

export interface AreaStat {
  area: string;
  count: number;
}

/** Counts deliveries per neighbourhood (first address segment). */
export function topAreas(deliveries: Delivery[], limit = 4): AreaStat[] {
  const map = new Map<string, number>();

  for (const delivery of deliveries) {
    const area = delivery.address.split(/[،,·|/-]/)[0]?.trim();
    if (!area) continue;
    map.set(area, (map.get(area) ?? 0) + 1);
  }

  return Array.from(map.entries())
    .map(([area, count]) => ({ area, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, limit);
}
