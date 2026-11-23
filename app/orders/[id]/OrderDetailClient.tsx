'use client';

import { OrderDetail } from '@/components/OrderDetail';

export function OrderDetailClient({ id }: { id: string }) {
  return <OrderDetail id={id} />;
}
