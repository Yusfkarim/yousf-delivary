'use client';

import { useEffect, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

interface SheetProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  subtitle?: string;
  children: ReactNode;
  footer?: ReactNode;
  /** fullscreen = near-full-height sheet (maps) */
  variant?: 'default' | 'full';
}

export function Sheet({ open, onClose, title, subtitle, children, footer, variant = 'default' }: SheetProps) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', onKeyDown);
    };
  }, [open, onClose]);

  if (!open || typeof document === 'undefined') return null;

  return createPortal(
    <div className="fixed inset-0 z-[70] flex items-end justify-center sm:items-center sm:p-4">
      <button
        type="button"
        aria-label="داخستن"
        onClick={onClose}
        className="absolute inset-0 h-full w-full cursor-default bg-ink-950/75 backdrop-blur-sm"
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`relative flex w-full flex-col overflow-hidden rounded-t-3xl border border-white/[0.09] bg-ink-850/95 shadow-[0_-20px_80px_-20px_rgba(0,0,0,0.95)] backdrop-blur-2xl animate-sheet-up sm:max-w-lg sm:rounded-3xl ${
          variant === 'full' ? 'max-h-[94dvh]' : 'max-h-[88dvh]'
        }`}
      >
        <div className="relative flex items-center gap-3 border-b border-white/[0.06] px-4 pb-3 pt-4">
          <span className="absolute left-1/2 top-1.5 h-1 w-10 -translate-x-1/2 rounded-full bg-white/15 sm:hidden" />
          <div className="min-w-0 flex-1">
            {title && <h2 className="truncate text-[15px] font-bold text-white">{title}</h2>}
            {subtitle && <p className="mt-0.5 truncate text-[11px] text-slate-400">{subtitle}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-white/10 bg-white/[0.05] text-slate-300 transition active:scale-95 hover:bg-white/10"
            aria-label="داخستن"
          >
            <X className="h-4 w-4" aria-hidden />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-4">{children}</div>

        {footer && (
          <div className="border-t border-white/[0.06] bg-ink-900/60 px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
            {footer}
          </div>
        )}
      </div>
    </div>,
    document.body,
  );
}
