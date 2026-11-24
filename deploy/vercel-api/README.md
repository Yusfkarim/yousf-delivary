# deploy/vercel-api — API bootstrap (شیم)

پرۆژەی Vercel ی `yousif-delivery-api` بە GitHub نەبەستراوە، بۆیە **تەنها ئەم
فایلە** (`setup.mjs`) ئەپلۆد دەکرێت. لە کاتی build دوو جار ئیش دەکات:

| قۆناغ | Command | ئەنجام |
| --- | --- | --- |
| Install | `node setup.mjs install` | `backend/` لە GitHub دەهێنێتەوە بۆ ڕەگی پرۆژەکە، پاشان `npm install` |
| Build | `node setup.mjs build && next build` | دڵنیا دەبێتەوە کە سەرچاوە هەیە، پاشان `next build` |

## ڕێکخستنی پرۆژە

| | |
| --- | --- |
| Framework | `nextjs` |
| Install Command | `node setup.mjs install` |
| Build Command | `node setup.mjs build && next build` |
| Root Directory | (بەتاڵ) |

## Env

| ناو | نرخ |
| --- | --- |
| `DELIVERY_REPO` | `Yusfkarim/yousf-delivary` (بنەڕەت) |
| `DELIVERY_REF` | `main` (بنەڕەت) |
| `DELIVERY_SUPABASE_URL` | `https://<ref>.supabase.co` |
| `DELIVERY_SUPABASE_SERVICE_KEY` | service_role key |
| `POSTGRES_URL` | ئارەزوومەندانە — ئەگەر هەبێت پێش Supabase بەکاردێت |

کۆدی سێرڤەر لە `backend/` ە و خۆی باکئێندەکە هەڵدەبژێرێت
(Postgres یان Supabase REST). بۆ دڵنیابوونەوە:

```bash
curl 'https://yousif-delivery-api.vercel.app/api/health?selftest=1&password=12345678rk&write=1'
```

## تاقیکردنەوەی ناوخۆیی

```bash
mkdir -p /tmp/api-test && cp deploy/vercel-api/setup.mjs /tmp/api-test/
cd /tmp/api-test
node setup.mjs install     # backend/ + node_modules
node setup.mjs build && ./node_modules/.bin/next build
```
