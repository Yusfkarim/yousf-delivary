import type { ReactNode } from 'react';

interface EmptyStateProps {
  icon: ReactNode;
  title: string;
  body?: string;
  action?: ReactNode;
}

export function EmptyState({ icon, title, body, action }: EmptyStateProps) {
  return (
    <div className="glass mt-6 flex flex-col items-center gap-3 px-6 py-10 text-center animate-fade-up">
      <span className="relative grid h-16 w-16 place-items-center rounded-3xl border border-white/10 bg-gradient-to-br from-neon/15 via-ink-800 to-gold/10 text-neon-soft">
        <span className="absolute inset-0 rounded-3xl border border-neon/10" />
        {icon}
      </span>
      <h3 className="text-[15px] font-bold text-white">{title}</h3>
      {body && <p className="max-w-xs text-[12.5px] leading-6 text-slate-400">{body}</p>}
      {action && <div className="pt-1">{action}</div>}
    </div>
  );
}
