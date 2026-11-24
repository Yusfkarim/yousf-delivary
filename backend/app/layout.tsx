import type { ReactNode } from 'react';

export const metadata = {
  title: 'یوسف دلیڤەری · API',
  description: 'سێرڤەری پارێزراوی کڕیارەکان بۆ ئەپی یوسف دلیڤەری',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ckb" dir="rtl">
      <body
        style={{
          margin: 0,
          minHeight: '100vh',
          background: '#090D16',
          color: '#E5E7EB',
          fontFamily: 'system-ui, "Noto Sans Arabic", sans-serif',
        }}
      >
        {children}
      </body>
    </html>
  );
}
