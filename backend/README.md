# یوسف دلیڤەری · Backend (Next.js + Vercel Postgres)

API ی پارێزراو بۆ ئەپی ئەندرۆید. هەموو داواکارییەک بە هێدەری
`x-app-password` دەپشکنرێت (constant-time، `crypto.timingSafeEqual`).

## خشتە (`customers`)

| ستوون | جۆر | تێبینی |
| --- | --- | --- |
| `id` | `TEXT PRIMARY KEY` | UUID لە کلاینتەوە — بۆیە دووبارەکردنەوە هیچ کاتێک داتای دووبارە دروست ناکات |
| `name` | `TEXT NOT NULL` | |
| `primary_phone` | `TEXT NOT NULL` | |
| `has_whatsapp` | `BOOLEAN NOT NULL DEFAULT FALSE` | |
| `has_viber` | `BOOLEAN NOT NULL DEFAULT FALSE` | |
| `extra_phones` | `JSONB NOT NULL DEFAULT '[]'` | `[{ "number", "has_whatsapp", "has_viber" }]` |
| `latitude` / `longitude` | `DOUBLE PRECISION NOT NULL` | |
| `address_note` | `TEXT NOT NULL DEFAULT ''` | |
| `created_at` / `updated_at` | `BIGINT NOT NULL` | میلی‌ثانیە |

خشتەکە لە یەکەم داواکاریدا خۆکارانە دروست دەکرێت (`CREATE TABLE IF NOT EXISTS`)،
هەروەها `CREATE INDEX IF NOT EXISTS customers_updated_at_idx`.

## API

| Method | Route | وەڵام |
| --- | --- | --- |
| `POST` | `/api/auth/verify` | `{ authenticated: true }` · `401` |
| `GET` | `/api/customers` | `{ customers: [...], count }` |
| `POST` | `/api/customers` | Upsert (`ON CONFLICT (id) DO UPDATE`) → `{ customer }` |
| `PUT` | `/api/customers/[id]` | `{ customer }` · `404` |
| `DELETE` | `/api/customers/[id]` | `{ ok: true, deleted: true, id }` · `404` |
| `GET` | `/api/health` | `{ ok: true, databaseConfigured }` (بێ وشەی نهێنی) |

کۆدی هەڵە: `401` وشەی نهێنی، `422` پشکنینی خانەکان، `503` داتابەیس نەبەستراوە،
`500` کێشەی داتابەیس.

```bash
curl -X POST https://<your-app>.vercel.app/api/auth/verify \
  -H 'x-app-password: 12345678rk'

curl https://<your-app>.vercel.app/api/customers \
  -H 'x-app-password: 12345678rk'
```

## ٣ هەنگاو بۆ deploy

1. **ناوەند**: لە Vercel → **Storage → Create → Postgres** داتابەیسێک دروست بکە
   و بە پرۆژەکەوە بیبەستەوە (ئەمە `POSTGRES_URL` خۆکارانە دادەنێت).
2. **پرۆژە**: ئەم فۆڵدەرە (`backend/`) وەک پرۆژەیەکی Next.js deploy بکە
   (یان لە ڕێگەی گیتەوە: *Import Project* → Root Directory = `backend`).
3. **ژینگە**: لە *Settings → Environment Variables* دایبنێ:
   - `POSTGRES_URL` (خۆکارانە لە هەنگاوی ١)
   - `APP_PASSWORD=12345678rk` (ئیختیاری — بەتاڵ بێت ئەمە بنەڕەتە)
   پاشان **Redeploy**.

> نیشانی سێرڤەر لە ئەپەکەدا لە **ڕێکخستن → نیشانی سێرڤەر** دەگۆڕدرێت،
> هەروەها لە `android-app/app/build.gradle.kts` ناو `API_BASE_URL`.

## تاقیکردنەوە

```bash
npm install
npm run typecheck
npm run build
npm run dev          # http://localhost:3000
```
