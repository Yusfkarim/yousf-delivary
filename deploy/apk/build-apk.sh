#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Builds the YousifDelivery release APK inside a Vercel build container
# (no GitHub Actions required). Output: apk-out/YousifDelivery-v<version>.apk
#
# Used by the Vercel project `yousif-delivary-apk`:
#   Install Command: echo "apk build: install skipped"
#   Build Command:   bash deploy/apk/build-apk.sh
#   Output dir:      apk-out
# ---------------------------------------------------------------------------
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APP="$ROOT/android-app"
TOOLS="${HOME}/tools"
OUT="$ROOT/apk-out"
GRADLE_VER="8.9"
ANDROID_HOME="${TOOLS}/android-sdk"
JDK_URL="https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VER}-bin.zip"
SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

step() { printf '\n=== %s ===\n' "$*"; }
die() { printf '\nBUILD FAILED: %s\n' "$*"; exit 1; }
fetch() { curl -fsSL --retry 3 --retry-delay 2 --max-time 1200 -o "$2" "$1" || die "download failed: $1"; }

# `jar xf` does not restore the executable bit, so chmod after every extract.
extract_zip() {
  local zip="$1" dest="$2"
  rm -rf "$dest"; mkdir -p "$dest"
  ( cd "$dest" && jar xf "$zip" ) || die "zip extract failed: $zip"
  chmod -R u+x "$dest"
}

mkdir -p "$TOOLS" "$OUT"

step "environment"
df -h / | tail -1
( node -v 2>/dev/null || true )
echo "root=$ROOT"

step "1/6 JDK 17"
if [ ! -x "$TOOLS/jdk/bin/java" ]; then
  fetch "$JDK_URL" "$TOOLS/jdk.tar.gz"
  rm -rf "$TOOLS/jdk" "$TOOLS/jdk-extract"
  mkdir -p "$TOOLS/jdk-extract"
  tar -xzf "$TOOLS/jdk.tar.gz" -C "$TOOLS/jdk-extract" || die "jdK extract"
  mv "$TOOLS"/jdk-extract/jdk-* "$TOOLS/jdk" || die "jdk move"
  rm -rf "$TOOLS/jdk.tar.gz" "$TOOLS/jdk-extract"
fi
export JAVA_HOME="$TOOLS/jdk"
export PATH="$JAVA_HOME/bin:$PATH"
java -version 2>&1 | head -1 || die "java not available"

step "2/6 Gradle ${GRADLE_VER}"
if [ ! -x "$TOOLS/gradle/bin/gradle" ]; then
  fetch "$GRADLE_URL" "$TOOLS/gradle.zip"
  extract_zip "$TOOLS/gradle.zip" "$TOOLS/gradle-extract"
  mv "$TOOLS/gradle-extract/gradle-${GRADLE_VER}" "$TOOLS/gradle" || die "gradle move"
  chmod -R u+x "$TOOLS/gradle/bin"
  rm -rf "$TOOLS/gradle.zip" "$TOOLS/gradle-extract"
fi
"$TOOLS/gradle/bin/gradle" --version | sed -n '1,4p' || die "gradle not runnable"

step "3/6 Android SDK"
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  fetch "$SDK_URL" "$TOOLS/cmdtools.zip"
  extract_zip "$TOOLS/cmdtools.zip" "$TOOLS/cmdtools"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  mv "$TOOLS/cmdtools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" || die "cmdtools move"
  chmod -R u+x "$ANDROID_HOME/cmdline-tools/latest/bin"
  rm -rf "$TOOLS/cmdtools.zip" "$TOOLS/cmdtools"
fi
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
( yes 2>/dev/null | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null 2>&1 ) || true
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" --install \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0" > "$TOOLS/sdk.log" 2>&1 || {
    tail -40 "$TOOLS/sdk.log"; die "sdkmanager install"; }
ls "$ANDROID_HOME/platforms" "$ANDROID_HOME/build-tools"
echo "sdk.dir=$ANDROID_HOME" > "$APP/local.properties"

step "4/6 release keystore"
KS="$APP/keystore/yousif-release.jks"
mkdir -p "$(dirname "$KS")"
if [ -n "${KEYSTORE_BASE64:-}" ]; then
  printf '%s' "$KEYSTORE_BASE64" | base64 -d > "$KS" || die "keystore decode"
  echo "keystore restored from KEYSTORE_BASE64"
elif [ -f "$APP/release-keystore.b64" ]; then
  tr -d '\n' < "$APP/release-keystore.b64" | base64 -d > "$KS" || die "keystore decode (repo)"
  echo "keystore restored from android-app/release-keystore.b64"
else
  keytool -genkeypair -keystore "$KS" -storetype PKCS12 \
    -alias "${KEY_ALIAS:-yousif}" -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "${KEYSTORE_PASSWORD:-yousif12345}" \
    -keypass "${KEY_PASSWORD:-yousif12345}" \
    -dname "CN=Yousif Delivery, O=Yousif, L=Sulaymaniyah, C=IQ" >/dev/null 2>&1 || die "keytool"
  echo "YOUSIF_KEYSTORE_B64_START"
  base64 -w0 "$KS" 2>/dev/null || base64 "$KS"
  printf '\nYOUSIF_KEYSTORE_B64_END\n'
fi
export KEYSTORE_PATH="keystore/yousif-release.jks"
export KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-yousif12345}"
export KEY_ALIAS="${KEY_ALIAS:-yousif}"
export KEY_PASSWORD="${KEY_PASSWORD:-yousif12345}"

step "5/6 assembleRelease"
cd "$APP" || die "app dir"
"$TOOLS/gradle/bin/gradle" --no-daemon --console=plain --stacktrace \
  -Dorg.gradle.jvmargs="-Xmx3g -Dfile.encoding=UTF-8" \
  :app:assembleRelease > "$TOOLS/gradle.log" 2>&1
RC=$?
tail -20 "$TOOLS/gradle.log"
if [ "$RC" -ne 0 ]; then
  echo "--- errors ---"
  grep -E "^e: |^w: .*(deprecated|unresolved)|error:|FAILURE:|What went wrong" "$TOOLS/gradle.log" | head -60
  die "gradle assembleRelease (rc=$RC)"
fi

step "6/6 publish"
VN="$(sed -n 's/.*versionName *= *"\([^"]*\)".*/\1/p' "$APP/app/build.gradle.kts" | head -1)"
VN="${VN:-1.0.0}"
VC="$(sed -n 's/.*versionCode *= *\([0-9]*\).*/\1/p' "$APP/app/build.gradle.kts" | head -1)"
VC="${VC:-1}"
APK="$(ls "$APP"/app/build/outputs/apk/release/*.apk 2>/dev/null | head -1)"
[ -n "$APK" ] || die "no apk produced"
NAME="YousifDelivery-v${VN}.apk"
cp "$APK" "$OUT/$NAME"
cp "$APK" "$OUT/YousifDelivery-latest.apk"
printf 'version=%s versionCode=%s\n' "$VN" "$VC"
sha256sum "$OUT/$NAME" | tee "$OUT/$NAME.sha256"
ls -lh "$OUT"

cat > "$OUT/index.html" <<HTML
<!doctype html>
<html lang="ku" dir="rtl"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>یوسف دلیڤەری · APK</title>
<style>
 body{background:#090D16;color:#E5EDF7;font-family:system-ui,-apple-system,"Noto Kufi Arabic",sans-serif;
      display:flex;min-height:100vh;align-items:center;justify-content:center;margin:0;text-align:center}
 .card{background:#111827;border:1px solid #1B2433;border-radius:22px;padding:34px 28px;max-width:420px}
 h1{margin:0 0 6px;font-size:24px}
 p{color:#93A4BC;font-size:14px;margin:8px 0 22px}
 a{display:block;background:#06B6D4;color:#04121A;font-weight:700;text-decoration:none;
   padding:16px;border-radius:16px;font-size:17px}
 small{display:block;margin-top:16px;color:#6B7A90;font-size:12px;word-break:break-all}
</style></head><body><div class="card">
<h1>یوسف دلیڤەری</h1>
<p>وەشانی ${VN} (${VC}) — APK ی ئەندرۆید</p>
<a href="./${NAME}" download>داگرتنی ئەپەکە (${NAME})</a>
<small>وشەی نهێنی: 12345678rk</small>
</div></body></html>
HTML

step "done"
echo "apk-out/$NAME ready"
