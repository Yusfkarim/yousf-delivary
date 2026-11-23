import clsx from 'clsx';
import { statusMeta } from '@/lib/i18n';
import type { DeliveryStatus } from '@/lib/types';

export function StatusBadge({
  status,
  short = false,
  className,
}: {
  status: DeliveryStatus;
  short?: boolean;
  className?: string;
}) {
  const meta = statusMeta[status];
  return (
    <span className={clsx('chip', meta.chip, className)}>
      <span
        className={clsx(
          'h-1.5 w-1.5 rounded-full',
          meta.dot,
          status === 'on_the_way' && 'animate-pulse',
        )}
      />
      {short ? meta.shortLabel : meta.label}
    </span>
  );
}
