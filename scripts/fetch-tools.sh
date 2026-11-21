#!/usr/bin/env bash
# Restores the offline toolchain used by build.sh (idempotent).
# Used both locally and on GitHub Actions runners.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$ROOT/tools"
mkdir -p "$TOOLS"

command -v unzip >/dev/null || { sudo apt-get update -qq && sudo apt-get install -y -qq unzip zip; }
command -v zip >/dev/null || { sudo apt-get update -qq && sudo apt-get install -y -qq zip; }

# 1) Java runtime + keytool (PyPI wheel: jdk4py)
if [ ! -x "$TOOLS/jdk4py/jdk4py/java-runtime/bin/java" ]; then
  echo "==> fetching JDK runtime (jdk4py)"
  (pip download jdk4py==21.0.8.2 --no-deps -d "$TOOLS" -q \
   || pip3 download jdk4py==21.0.8.2 --no-deps -d "$TOOLS" -q)
  WHEEL=$(ls "$TOOLS"/jdk4py-21.0.8.2-*.whl | head -1)
  unzip -o -q "$WHEEL" -d "$TOOLS/jdk4py"
  rm -f "$WHEEL"
fi

# 2) aapt2 (linux x64 prebuilt)
if [ ! -x "$TOOLS/aapt2-bin/aapt2_64" ]; then
  echo "==> fetching aapt2"
  rm -rf "$TOOLS/aapt2-repo"
  git clone --depth 1 \
    https://github.com/VindroidH/prebuilt-binaries-aapt-aapt_64-aapt2-aapt2_64-windows-linux-macosx.git \
    "$TOOLS/aapt2-repo"
  unzip -o -q "$TOOLS/aapt2-repo/linux/aapt2_64.zip" -d "$TOOLS/aapt2-bin"
  chmod +x "$TOOLS/aapt2-bin/aapt2_64"
fi

# 3) Eclipse Java compiler (ECJ)
if [ ! -f "$TOOLS/android-ide/Android IDE/libs/ecj.jar" ]; then
  echo "==> fetching ECJ"
  rm -rf "$TOOLS/android-ide"
  git clone --depth 1 --filter=blob:none --sparse \
    https://github.com/kedargn/IDEonAndroidForAndroidPlatform.git "$TOOLS/android-ide"
  git -C "$TOOLS/android-ide" sparse-checkout set "Android IDE/libs"
fi

# 4) android.jar + d8 + apksigner
if [ ! -f "$TOOLS/h2apk/tools/d8.jar" ]; then
  echo "==> fetching android.jar / d8 / apksigner"
  rm -rf "$TOOLS/h2apk"
  git clone --depth 1 --filter=blob:none --sparse \
    https://github.com/HashShin/H2APK.git "$TOOLS/h2apk"
  git -C "$TOOLS/h2apk" sparse-checkout set tools
fi

# sanity
for f in \
  "$TOOLS/jdk4py/jdk4py/java-runtime/bin/java" \
  "$TOOLS/jdk4py/jdk4py/java-runtime/bin/keytool" \
  "$TOOLS/aapt2-bin/aapt2_64" \
  "$TOOLS/android-ide/Android IDE/libs/ecj.jar" \
  "$TOOLS/h2apk/tools/android.jar" \
  "$TOOLS/h2apk/tools/d8.jar" \
  "$TOOLS/h2apk/tools/apksigner.jar"; do
  if [ ! -e "$f" ]; then
    echo "toolchain incomplete, missing: $f" >&2
    exit 1
  fi
done
echo "toolchain ready"
