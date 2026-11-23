'use client';

import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react';
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';

type ToastTone = 'success' | 'error' | 'info';

interface ToastItem {
  id: string;
  message: string;
  tone: ToastTone;
}

interface ToastContextValue {
  toast: (message: string, tone?: ToastTone) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const TONE_STYLES: Record<ToastTone, { border: string; icon: ReactNode; glow: string }> = {
  success: {
    border: 'border-success/40',
    icon: <CheckCircle2 className="h-5 w-5 text-success" aria-hidden />,
    glow: 'shadow-[0_18px_45px_-20px_rgba(52,211,153,0.65)]',
  },
  error: {
    border: 'border-danger/40',
    icon: <AlertCircle className="h-5 w-5 text-danger" aria-hidden />,
    glow: 'shadow-[0_18px_45px_-20px_rgba(244,63,94,0.65)]',
  },
  info: {
    border: 'border-neon/40',
    icon: <Info className="h-5 w-5 text-neon" aria-hidden />,
    glow: 'shadow-[0_18px_45px_-20px_rgba(6,182,212,0.65)]',
  },
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const timers = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());

  const remove = useCallback((id: string) => {
    setItems((prev) => prev.filter((item) => item.id !== id));
    const timer = timers.current.get(id);
    if (timer) {
      clearTimeout(timer);
      timers.current.delete(id);
    }
  }, []);

  const toast = useCallback(
    (message: string, tone: ToastTone = 'info') => {
      if (!message) return;
      const id = `t-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
      setItems((prev) => [...prev.slice(-2), { id, message, tone }]);
      const timer = setTimeout(() => remove(id), 3600);
      timers.current.set(id, timer);
    },
    [remove],
  );

  const value = useMemo(() => ({ toast }), [toast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        className="pointer-events-none fixed inset-x-0 bottom-24 z-[80] flex flex-col items-center gap-2 px-4 sm:bottom-8"
        role="status"
        aria-live="polite"
      >
        {items.map((item) => {
          const tone = TONE_STYLES[item.tone];
          return (
            <div
              key={item.id}
              className={`pointer-events-auto flex w-full max-w-sm items-center gap-3 rounded-2xl border ${tone.border} ${tone.glow} bg-ink-850/90 px-4 py-3 backdrop-blur-xl animate-fade-up`}
            >
              {tone.icon}
              <p className="flex-1 text-sm leading-6 text-slate-100">{item.message}</p>
              <button
                type="button"
                onClick={() => remove(item.id)}
                className="rounded-full p-1 text-slate-400 transition hover:bg-white/5 hover:text-slate-100"
                aria-label="داخستن"
              >
                <X className="h-4 w-4" aria-hidden />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used inside ToastProvider');
  return context;
}
