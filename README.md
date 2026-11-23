# یوسف دلیڤەری · Yousif Delivery

سیستەمی بەڕێوەبردنی گەیاندن بۆ شۆفێرانی کوردستان — mobile-first، تەواو بە کوردی سۆرانی (RTL)،
PWA و ئامادە بۆ کارکردن لەسەر مۆبایل و بێ ئینتەرنێت.

A production-ready delivery management PWA for couriers in Kurdistan/Iraq: Sorani Kurdish RTL
interface, Vercel Postgres storage, Leaflet/OpenStreetMap live routes, one-tap calling /
WhatsApp / Viber, GPS pinning, earnings dashboard and offline-first caching.

---

## ✨ Features

### Customers & orders
- Relational `customers` + `orders` schema (JSONB extra phones, GPS coordinates, landmark,
  price, delivery fee, currency, status, timestamps).
- **Unlimited extra phone numbers** per customer with a `+٢ ژمارەی تر` expandable row, each with
  its own Call / WhatsApp / Viber buttons.
- Automatic Iraqi phone normalisation: `0750 123 4567`, `+964 750 123 4567`, `00964…`,
  `964…` all become `+9647501234567`; deep links use `tel:`, `https://wa.me/964…`,
  `viber://chat?number=%2B964…`.
- WhatsApp / Viber badges so the driver knows at a glance which app the customer uses.

### GPS, maps and routing
- **دیاریکردنی شوێنی من بە GPS** — one tap to pin the current location.
- Interactive map picker: tap anywhere to drop the pin, paste a Google Maps / Apple Maps /
  OSM link (coordinates are extracted automatically), or search a place by name.
- Live Haversine distance on every card (`٢.٤ کم دوورە لێتەوە`) + ETA, recalculated as the
  driver moves.
- Location badge opens a route sheet with **my live location** and **customer location**,
  a real road route (OSRM, free, no key) with straight-line fallback, Waze / Apple Maps
  deep links and **دەستپێکردنی ڕێگا (Google Maps)** turn-by-turn navigation.
- Dark CARTO/OpenStreetMap tiles, glowing custom pins — no API keys required anywhere.

### Speed for daily work
- Today's dashboard: deliveries completed, pending/on-the-way count, cash collected (IQD).
- Instant fuzzy search across name, **all** phone numbers, neighbourhood, landmark and item
  note — tolerant to Arabic/Kurdish letter variants (`ي/ی`, `ك/ک`, `ە/ه`, Arabic-Indic digits).
- Status filter pills + sort by **نزیکترین لێمەوە** (nearest first) or **نوێترین** (newest).
- Pre-filled Kurdish WhatsApp ETA message and delivered message templates.
- One-click copy of address, coordinates and phone numbers.
- Earnings dashboard with period tabs, 7-day chart, top neighbourhoods and recent activity.

### Offline-first PWA
- Installable on Android/iOS home screen (manifest, icons, standalone display, shortcuts).
- Service worker caches the app shell and static assets; navigations are network-first with an
  offline fallback page.
- Orders are cached in `localStorage` and rendered instantly on launch; writes made while
  offline go to an **outbox** and sync automatically when the connection returns.
- Weak-signal friendly: cached numbers, addresses and pins stay available.

---

## 🧱 Tech stack

| Layer | Choice |
| --- | --- |
| Framework | Next.js 15 (App Router) + React 19 + TypeScript (strict) |
| Styling | Tailwind CSS 3.4, dark glassmorphism (deep slate `#090D16`, neon cyan `#06B6D4`, gold `#F59E0B`) |
| Icons / fonts | Lucide icons, self-hosted Noto Sans Arabic + Vazirmatn |
| Database | Vercel Postgres (`@vercel/postgres`) with auto-created schema; JSON file fallback for local demos |
| Maps | Leaflet + react-leaflet, CARTO dark tiles / OpenStreetMap |
| Routing | OSRM public demo server, Google Maps & Waze deep links |
| PWA | Hand-written `public/sw.js` + `public/manifest.webmanifest` |

No external API keys, no paid services, no tracking.

---

## 🚀 Quick start

```bash
npm install
cp .env.example .env.local   # optional: add POSTGRES_URL
npm run dev                  # http://localhost:3000
```

Without `POSTGRES_URL` the app uses a local JSON store (`.data/store.json`) that is seeded with
eight realistic Sulaymaniyah demo deliveries — handy for design review and offline demos.
With `POSTGRES_URL` set, the real schema is created automatically on the first request and the
database starts empty (use **ڕێکخستن → دانانی داتای نموونە** or `SEED_DEMO=true` to seed).

### Scripts

| Script | What it does |
| --- | --- |
| `npm run dev` | Dev server |
| `npm run build` / `npm start` | Production build / serve |
| `npm run typecheck` | Strict TypeScript check |
| `npm run selfcheck` | Logic test suite (phones, geo, search, validation, SQL, storage) |
| `npm run schema:build` | Regenerate `schema.sql` from `lib/db/schema.ts` |
| `npm run icons` | Regenerate the PWA icon set from `public/icons/logo.svg` |

---

## 🗄️ Database

Set one of these environment variables (checked in this order):

```
POSTGRES_URL            # injected by Vercel Postgres
DATABASE_URL            # any Postgres (Neon, Supabase, local)
POSTGRES_PRISMA_URL
POSTGRES_URL_NON_POOLING
```

The schema is created on the first query by `lib/db/postgres.ts` (idempotent, split into
single statements for the Neon driver). To apply it manually:

```bash
psql "$POSTGRES_URL" -f schema.sql
```

Tables:

```
customers(id, name, primary_phone, extra_phones JSONB, latitude, longitude,
          address, landmark, channels TEXT[], notes, created_at, updated_at)
orders   (id, order_no SERIAL, customer_id → customers, item_note, price NUMERIC,
          currency, delivery_fee, status, created_at, updated_at, delivered_at)
```

`status ∈ {pending, on_the_way, delivered, canceled}`, `currency ∈ {IQD, USD}`.
Indexes on status, created_at, delivered_at, customer_id and primary_phone; `updated_at` is
maintained by a trigger.

---

## ☁️ Deploy to Vercel (1-click)

1. Push the repository to GitHub.
2. In Vercel: **Add New → Project → Import** the repo (framework auto-detected).
3. **Storage → Create → Postgres** (or connect Neon from the Marketplace) and attach it to the
   project — `POSTGRES_URL` is injected automatically.
4. (Optional) add `SEED_DEMO=true` for a demo deployment.
5. Deploy. Open the URL on a phone and use **زیادکردن بە شاشەی سەرەکی** to install the PWA.

### Environment variables

| Variable | Required | Description |
| --- | --- | --- |
| `POSTGRES_URL` | recommended | Postgres connection string (Vercel injects it) |
| `DATABASE_URL` | alternative | Any Postgres connection string |
| `SEED_DEMO` | optional | `true` → seed sample deliveries when the database is empty |
| `APP_PIN` | optional | Gate the whole app behind a numeric PIN (recommended for public URLs) |
| `NEXT_PUBLIC_DEFAULT_CURRENCY` | optional | `IQD` (default) or `USD` |
| `DATA_DIR` | optional | Directory for the JSON fallback store (defaults to `.data` or `/tmp` on Vercel) |

### ئامادەکردنی ئێستا · Live deployment

| | |
| --- | --- |
| ناونیشان | **https://yousf27.vercel.app** |
| پرۆژە | `yousf27` (team `yusfkarim2001-1023's projects`) |
| دۆخ | `READY` — production alias |

ئەم پرۆژەیە هێشتا لە Vercel بە GitHub نەبەستراوە، بۆیە deployment ەکە لە ڕێگەی
`deploy/vercel/setup.mjs` ئەنجام دراوە:

- `Install Command` → `node setup.mjs install` (package.json + `npm install`)
- `Build Command` → `node setup.mjs build && next build` (سەرچاوە لە GitHub دادەبەزێنێت)

دوای بەستنەوەی **Vercel → Account Settings → Login Connections → GitHub**، دەتوانرێت
ڕاستەوخۆ لە گیت deploy بکرێت و ئەم شیمە لاببرێت. هەتا ئەو کاتە، هەر جارێک کۆدی نوێ
push بکرێت پێویستی بە **Redeploy** هەیە (یەک کلیک لە داشبۆرد یان لە ڕێگەی ئێمەوە).

> ⚠️ دوو خاڵی گرنگ بۆ بەرهەمهێنان:
> 1. **Deployment Protection** — بە شێوەی سەرەتایی «Vercel Authentication» چالاکە، بۆیە
>    تەنها ئەندامانی هەژمار دەتوانن بیبینن. بۆ هاوبەشکردن لەگەڵ شۆفێرەکان:
>    **Project → Settings → Deployment Protection → Vercel Authentication → Disable**.
> 2. **داتابەیس** — بەبێ `POSTGRES_URL` ئەپەکە بە کۆگای JSON لە `/tmp` کار دەکات
>    (داتا لەگەڵ هەر cold start دەسڕێتەوە). بۆ داتای هەمیشەیی:
>    **Storage → Create → Postgres** و پاشان **Redeploy**.

---

## 🔌 API

| Method | Route | Description |
| --- | --- | --- |
| `GET` | `/api/deliveries?limit=500` | Joined orders + customers, newest first |
| `POST` | `/api/deliveries` | Create a customer + order in one transaction (or add an order to `customerId`) |
| `GET` | `/api/deliveries/:id` | Single delivery |
| `PATCH` | `/api/deliveries/:id` | Partial update (status-only updates are optimised) |
| `DELETE` | `/api/deliveries/:id` | Delete the order (and orphaned customer) |
| `POST` | `/api/demo` | Seed sample data when empty |
| `GET` | `/api/health` | Backend, schema state and record count |

Errors are localised: `{ "error": "validation_failed", "message": "…", "fields": { … } }`.

---

## 🗂 Project structure

```
app/
  layout.tsx            RTL shell, metadata, manifest, providers
  page.tsx              Dashboard: stats, search, filters, delivery cards
  orders/new/           New order (GPS + map + phones + price)
  orders/[id]/          Detail, status timeline, contact + navigation actions
  orders/[id]/edit/     Edit order
  map/                  Full-screen map of all deliveries
  stats/                Earnings dashboard
  settings/             Driver, appearance, install, data & sync, about
  offline/              Service-worker offline fallback
  api/                  Route handlers (deliveries, demo, health)
components/             DeliveryCard, RouteModal, MapPicker, OrderForm, UI kit
hooks/                  GPS watch, online status
lib/                    Types, i18n (Kurdish copy deck), phone/geo/format helpers,
                        search, stats, validation, store (offline outbox), db layer
public/                 Manifest, service worker, icon set, logo
scripts/                schema generator, icon renderer, self-check suite
schema.sql              Generated DDL (also applied automatically at runtime)
```

---

## 🔒 Notes on data & privacy

- Customer names, phone numbers and GPS pins are personal data: keep the deployment private.
  Set `APP_PIN` to require a numeric PIN before the app (and API) responds — visitors are sent to
  `/unlock`, and API calls without a valid session cookie get `401`.
- Delete an order to remove it; when a customer has no remaining orders their row is removed too
  (`ON DELETE CASCADE` on orders).
- The browser stores cached orders in `localStorage`; **ڕێکخستن → سڕینەوەی کاشی ناوخۆیی**
  clears it instantly.

---

## 🧪 Quality gates

```bash
npm run typecheck   # 0 TypeScript errors
npm run selfcheck   # 11/11 logic tests
npm run build       # production build, no warnings
```

Made with خۆشەویستی for the couriers of Kurdistan.
