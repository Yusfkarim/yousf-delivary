'use client';

import Link from 'next/link';
import { ArrowRight, Loader2, PackageX } from 'lucide-react';
import { useStore } from '@/lib/store';
import { OrderForm } from '@/components/OrderForm';
import { EmptyState } from '@/components/ui/EmptyState';
import { t } from '@/lib/i18n';

export function EditOrderClient({ id }: { id: string }) {
  const { deliveries, loading, getDelivery } = useStore();
  const delivery = getDelivery(id) ?? deliveries.find((item) => item.id === id);

  if (!delivery && loading) {
    return (
      <div className="grid place-items-center py-24 text-slate-400">
        <Loader2 className="h-6 w-6 animate-spin text-neon" aria-hidden />
        <p className="mt-2 text-[12px]">{t.loading}</p>
      </div>
    );
  }

  if (!delivery) {
    return (
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

  return <OrderForm mode="edit" delivery={delivery} />;
}
