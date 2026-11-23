# deploy/vercel — Vercel bootstrap (شیم)

ئەم فۆڵدەرە تەنها بۆ حاڵەتێکە کە پرۆژەی Vercel بە GitHub نەبەستراوە.

## چۆن کار دەکات

`setup.mjs` یەک فایلە و دوو جار لە کاتی build بەکاردێت:

| قۆناغ | Command | ئەنجام |
| --- | --- | --- |
| Install | `node setup.mjs install` | `package.json` دەنووسێت و `npm install` ئەکات |
| Build | `node setup.mjs build && next build` | سەرچاوەی `main` لە GitHub دەکاتەوە، پاشان `next build` |

ئەم دوو ڕێکخستنە لە **Project → Settings → Build & Development Settings** دانرێن
(یان لە کاتی دروستکردنی deployment بە API).

## Deploy ەکەی ئێستا

| | |
| --- | --- |
| Project | `yousf27` |
| Domain | https://yousf27.vercel.app |
| Deployment | `dpl_HB8Xc279zd5sWFnPdXQB6LKXv16b` |

## دوای بەستنەوەی GitHub

1. Vercel → **Account Settings → Login Connections → GitHub** → Connect.
2. Project → **Settings → Git → Connect Git Repository** → `Yusfkarim/yousf-delivary`.
3. Build Command بگەڕێنەوە بۆ `next build` و Install Command بۆ `npm install`.
4. ئەم فۆڵدەرە دەتوانرێت بسڕدرێتەوە.

## تاقیکردنەوەی ناوخۆیی

```bash
mkdir /tmp/shim-test && cp deploy/vercel/setup.mjs /tmp/shim-test/
cd /tmp/shim-test
node setup.mjs install     # package.json + node_modules
node setup.mjs build       # سەرچاوە لە GitHub
./node_modules/.bin/next build
```
