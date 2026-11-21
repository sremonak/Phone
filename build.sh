#!/usr/bin/env bash
# Aurora Phone — full APK build without Gradle.
# Pipeline: aapt2 (resources) -> ECJ (javac) -> d8 (dex) -> apksigner (v2/v3).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
TOOLS="$ROOT/tools"
JAVA_BIN="$TOOLS/jdk4py/jdk4py/java-runtime/bin/java"
KEYTOOL="$TOOLS/jdk4py/jdk4py/java-runtime/bin/keytool"
AAPT2="$TOOLS/aapt2-bin/aapt2_64"
ECJ="$TOOLS/android-ide/Android IDE/libs/ecj.jar"
ANDROID_JAR="$TOOLS/h2apk/tools/android.jar"
D8="$TOOLS/h2apk/tools/d8.jar"
APKSIGNER="$TOOLS/h2apk/tools/apksigner.jar"
APP="$ROOT/app"
BUILD="$ROOT/build"
DIST="$ROOT/dist"
VERSION="1.1.0"
VERSION_CODE=2
MIN_SDK=26
TARGET_SDK=34
OUT_NAME="aurora-phone-v${VERSION}.apk"

for tool in "$JAVA_BIN" "$KEYTOOL" "$AAPT2" "$ECJ" "$ANDROID_JAR" "$D8" "$APKSIGNER"; do
  if [ ! -e "$tool" ]; then
    echo "missing tool: $tool" >&2
    exit 1
  fi
done

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$DIST"

echo "==> [1/8] compiling resources"
"$AAPT2" compile --dir "$APP/res" -o "$BUILD/res.flata"

echo "==> [2/8] linking manifest + resources"
"$AAPT2" link \
  --manifest "$APP/AndroidManifest.xml" \
  -I "$ANDROID_JAR" \
  -R "$BUILD/res.flata" \
  --auto-add-overlay \
  --min-sdk-version "$MIN_SDK" \
  --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION" \
  --java "$BUILD/gen" \
  -o "$BUILD/base.apk"

echo "==> [3/8] compiling java (ECJ)"
SOURCES=$(find "$APP/src" "$BUILD/gen" -name '*.java')
# shellcheck disable=SC2086
"$JAVA_BIN" -jar "$ECJ" \
  -source 1.6 -target 1.6 \
  -bootclasspath "$ANDROID_JAR" \
  -nowarn -proc:none \
  -d "$BUILD/classes" $SOURCES

echo "==> [4/8] dexing (d8)"
(cd "$BUILD/classes" && zip -qr "$BUILD/classes.jar" .)
"$JAVA_BIN" -cp "$D8" com.android.tools.r8.D8 \
  --release \
  --min-api "$MIN_SDK" \
  --lib "$ANDROID_JAR" \
  --output "$BUILD/dex" \
  "$BUILD/classes.jar"

echo "==> [5/8] packaging classes.dex"
(cd "$BUILD/dex" && zip -q "$BUILD/base.apk" classes.dex)

echo "==> [6/8] zipalign"
python3 "$ROOT/scripts/zipalign.py" "$BUILD/base.apk" "$BUILD/aligned.apk" 4

echo "==> [7/8] signing"
KS="$ROOT/keystore/aurora.jks"
if [ ! -f "$KS" ]; then
  mkdir -p "$ROOT/keystore"
  "$KEYTOOL" -genkeypair -keystore "$KS" -alias aurora \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android \
    -dname "CN=Aurora Phone, OU=Aurora, O=Aurora, L=New York, C=US"
fi
"$JAVA_BIN" -jar "$APKSIGNER" sign \
  --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$DIST/$OUT_NAME" "$BUILD/aligned.apk"

echo "==> [8/8] verifying"
"$JAVA_BIN" -jar "$APKSIGNER" verify --verbose "$DIST/$OUT_NAME"
"$AAPT2" dump badging "$DIST/$OUT_NAME" | sed -n '1,8p'
echo
echo "APK ready: $DIST/$OUT_NAME"
ls -la "$DIST/$OUT_NAME"
