import type { Metadata, Viewport } from 'next';
import '@fontsource/noto-sans-arabic/400.css';
import '@fontsource/noto-sans-arabic/500.css';
import '@fontsource/noto-sans-arabic/600.css';
import '@fontsource/noto-sans-arabic/700.css';
import '@fontsource/vazirmatn/400.css';
import '@fontsource/vazirmatn/700.css';
import './globals.css';
import 'leaflet/dist/leaflet.css';

import { AppShell } from '@/components/AppShell';
import { ToastProvider } from '@/components/ui/Toast';
import { StoreProvider } from '@/lib/store';
import { InstallProvider } from '@/components/pwa/InstallProvider';
import { PwaRegister } from '@/components/pwa/PwaRegister';
import { t } from '@/lib/i18n';

export const metadata: Metadata = {
  title: {
    default: `${t.appName} — گەیاندنی خێرا`,
    template: `%s · ${t.appName}`,
  },
  description:
    'سیستەمی بەڕێوەبردنی گەیاندن بۆ شۆفێرانی کوردستان: ناونیشان بە GPS، پەیوەندی بە واتساپ و ڤایبەر، ڕێنمایی نەخشە و ژمێریاری داهاتی ڕۆژانە.',
  applicationName: t.appName,
  manifest: '/manifest.webmanifest',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'black-translucent',
    title: t.appName,
  },
  formatDetection: { telephone: false, address: false, email: false },
  icons: {
    icon: [
      { url: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
      { url: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
    ],
    apple: [{ url: '/icons/apple-touch-icon.png', sizes: '180x180' }],
    shortcut: ['/icons/icon-192.png'],
  },
  other: {
    'mobile-web-app-capable': 'yes',
    'apple-mobile-web-app-capable': 'yes',
    'apple-mobile-web-app-status-bar-style': 'black-translucent',
  },
};

export const viewport: Viewport = {
  themeColor: '#090D16',
  width: 'device-width',
  initialScale: 1,
  viewportFit: 'cover',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ckb" dir="rtl" className="dark">
      <body>
        <ToastProvider>
          <InstallProvider>
            <StoreProvider>
              <AppShell>{children}</AppShell>
            </StoreProvider>
          </InstallProvider>
        </ToastProvider>
        <PwaRegister />
      </body>
    </html>
  );
}
