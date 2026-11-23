import type { Metadata } from 'next';
import { OrderDetailClient } from './OrderDetailClient';
import { t } from '@/lib/i18n';

export const metadata: Metadata = {
  title: t.orderDetailsTitle,
};

export default async function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <OrderDetailClient id={id} />;
}
