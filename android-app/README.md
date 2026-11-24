# یوسف دلیڤەری · Native Android (Kotlin + Compose)

ئەپی ناتیڤی RTL بۆ شۆفێرێکی تاک: ناو و ژمارەی کڕیار + شوێنی GPS.
**بێ نرخی داواکاری، بێ دۆخ، بێ داهات** — تەنها پەیوەندی و نەخشە.

## تەکنەلۆژیا

| بەش | هەڵبژاردە |
| --- | --- |
| زمان / UI | Kotlin 2.0.21 · Jetpack Compose · Material 3 · RTL |
| SDK | `compileSdk 35` · `targetSdk 35` · `minSdk 26` |
| هەڵگرتن | Room 2.6.1 (KSP) + WorkManager (retry کاتێک ئینتەرنێت دێتەوە) |
| تۆڕ | OkHttp + Gson · هێدەری `x-app-password` |
| نەخشە | osmdroid 6.1.20 + OpenStreetMap Mapnik (بێ API Key) |
| شوێن | FusedLocationProviderClient + LocationManager fallback |
| نهێنی | `EncryptedSharedPreferences` (MasterKey AES256-GCM) |

## بنیاتنانی APK لە Vercel (بێ GitHub Actions)

لەم ڕێپۆیەدا GitHub Actions ناچالاکە، بۆیە APK لە کۆنتەینەری build ی
Vercel دروست دەکرێت:

```bash
# پرۆژەی Vercel: yousif-delivary-apk
# Install:  echo apk-install-skip
# Build:    node setup-apk.mjs
# Output:   apk-out
```

`deploy/apk/build-apk.sh` خۆی JDK 17، Gradle 8.9 و Android SDK 35 (platform +
build-tools) دادەبەزێنێت، پاشان `assembleRelease` دەکات و APK دەخاتە
`apk-out/YousifDelivery-v<version>.apk`.

- کلیلی امضا لە `android-app/release-keystore.b64` هەڵدەگیرێت بۆ ئەوەی
  نوێکردنەوەکان بەسەرەوە دامەزرێن (وشەی نهێنی: `yousif12345`، alias: `yousif`).
- بۆ وەشانێکی نوێ: `versionCode += 1` و `versionName` بگۆڕە، پاشان project
  ەکە لە Vercel دووبارە deploy بکە.

## بنیاتنان

```bash
cd android-app
gradle :app:assembleDebug      # یان ./gradlew … ئەگەر wrapper ەکەیت
```

APK لە `app/build/outputs/apk/` دەردەچێت.

## وەشان و بڵاوکردنەوە

- `versionCode` و `versionName` لە `app/build.gradle.kts` — بۆ هەر وەشانێک
  `versionCode += 1` بکە.
- بە tag ێک (`v1.0.1`) push بکە → `.github/workflows/android-release.yml`
  خۆکارانە APK دروست دەکات و لە **GitHub Releases** بڵاوی دەکاتەوە
  بە ناوی `YousifDelivery-v<version>.apk`.
- یان دەستی: *Actions → Android Release → Run workflow*.

### امضا (signing)

CI یەکەم جار keystore یەکێک دروست دەکات و لە cache هەڵیدەگرێت
(`yousif-release-keystore-v1`) بۆ ئەوەی نوێکردنەوەکان بەسەرەوە دامەزرێن.
بۆ دڵنیایی هەمیشەیی، ئەم سێ Secret زیاد بکە:

| Secret | ناوەڕۆک |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -w0 keystore.jks` |
| `KEYSTORE_PASSWORD` | وشەی نهێنی keystore |
| `KEY_ALIAS` / `KEY_PASSWORD` | alias و وشەی نهێنی کلیل |

## شوێن و مۆڵەت

- `ACCESS_FINE_LOCATION` لە یەکەم کردنەوەدا داوا دەکرێت؛ ئەگەر ڕەت بکرێتەوە
  ئەپ کار دەکات بەڵام دووری نیشان نادرێت.
- دووری بە Haversine لە GPS ی ڕاستەقینەوە هەژمار دەکرێت.

## ڤایبەر / واتسئەپ

- واتسئەپ: `https://wa.me/9647xxxxxxxxx`
- ڤایبەر: `viber://add?number=9647xxxxxxxxx` (بێ `+`)
  پاشان `viber://contact?number=%2B9647xxxxxxxxx`، و ئەگەر دامەزراو نەبێت
  تووستی کوردی دەردەکەوێت.
- پەیوەندی: `Intent.ACTION_DIAL` (ڕاستەوخۆ بۆ دیالەر، بێ پێویستی CALL_PHONE).
- ناڤیگەیشن: `google.navigation:q=lat,lng` و fallback بۆ Google Maps وێب.

## هاوکاتکردن (zero data loss)

1. هەر پاشەکەوتکردنێک یەکسەر لە Room دەنووسرێت (`PENDING_UPSERT`).
2. هەمان کات داواکاری دەچێتە سێرڤەر → `SYNCED` و تووستی سەوز.
3. ئەگەر ئینتەرنێت نەبێت: `PENDING` + WorkManager (`NetworkType.CONNECTED`)
   دواتر خۆکارانە دەینێرێت. هیچ داتایەک نافڕێنرێت.
4. لە کاتی login، هەموو داتای سێرڤەر (`GET /api/customers`) دێتە خوارەوە —
   بۆیە دوای reinstall هەموو کڕیارەکان دەگەڕێنەوە.
