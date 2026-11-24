import { isConfigured } from '@/lib/db';

const rows: Array<{ method: string; path: string; note: string }> = [
  { method: 'POST', path: '/api/auth/verify', note: 'پشکنینی وشەی نهێنی' },
  { method: 'GET', path: '/api/customers', note: 'هەموو کڕیارەکان' },
  { method: 'POST', path: '/api/customers', note: 'زیادکردن / نوێکردنەوە (idempotent)' },
  { method: 'PUT', path: '/api/customers/[id]', note: 'دەستکاری' },
  { method: 'DELETE', path: '/api/customers/[id]', note: 'سڕینەوە' },
  { method: 'GET', path: '/api/health', note: 'پشکنینی ژیان (بێ وشەی نهێنی)' },
];

export default function Home() {
  const configured = isConfigured();
  return (
    <main style={{ maxWidth: 720, margin: '0 auto', padding: '48px 20px' }}>
      <h1 style={{ fontSize: 28, margin: 0, color: '#F8FAFC' }}>یوسف دلیڤەری</h1>
      <p style={{ color: '#94A3B8', marginTop: 8 }}>
        سێرڤەری کڕیارەکان · پارێزراو بە <code style={{ color: '#06B6D4' }}>x-app-password</code>
      </p>

      <div
        style={{
          marginTop: 24,
          padding: '14px 18px',
          borderRadius: 16,
          border: '1px solid rgba(148,163,184,0.25)',
          background: configured ? 'rgba(6,182,212,0.08)' : 'rgba(245,158,11,0.08)',
        }}
      >
        <strong style={{ color: configured ? '#22D3EE' : '#F59E0B' }}>
          {configured ? 'داتابەیس بەستراوەتەوە ✓' : 'داتابەیس نەبەستراوەتەوە'}
        </strong>
        <div style={{ color: '#94A3B8', fontSize: 14, marginTop: 6 }}>
          {configured
            ? 'هەموو داواکارییەکانی ئەپ کار دەکەن.'
            : 'لە Vercel → Storage → Create → Postgres داتابەیسێک دروست بکە و بە پرۆژەکەوە بیبەستەوە، پاشان Redeploy بکە.'}
        </div>
      </div>

      <table style={{ width: '100%', marginTop: 28, borderCollapse: 'collapse', fontSize: 14 }}>
        <tbody>
          {rows.map((row) => (
            <tr key={row.method + row.path} style={{ borderBottom: '1px solid rgba(148,163,184,0.15)' }}>
              <td style={{ padding: '10px 0', color: '#F59E0B', width: 80 }}>{row.method}</td>
              <td style={{ padding: '10px 0', color: '#E2E8F0', direction: 'ltr', textAlign: 'right' }}>
                {row.path}
              </td>
              <td style={{ padding: '10px 0', color: '#94A3B8' }}>{row.note}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
}
