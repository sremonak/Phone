# Aurora Phone — Premium Dialer & Caller ID

A modern, professional Android calling app with **detailed caller information**,
built without Gradle. Single APK, ~137 KB, zero runtime dependencies.

| | |
|---|---|
| Package | `com.aurora.phone` |
| Version | 1.0.0 (versionCode 1) |
| minSdk / targetSdk | 26 (Android 8.0) / 34 (Android 14) |
| Signature | APK Signature Scheme v2 + v3 |
| Output | `dist/aurora-phone-v1.0.0.apk` |

---

## Features

### Modern design
- Material-inspired UI with a **violet gradient identity**, rounded cards,
  ripple feedback, pill tabs and a floating keypad button
- **Light + dark themes** — follow system, or force either from Settings
  (themes are applied per-activity, so switching is instant and complete)
- Custom-drawn **gradient initial avatars** (no photo assets needed)
- Adaptive launcher icon (vector, anydpi-v26)

### Detailed caller info (the flagship screen)
Tap any number in Recents, Contacts or Favorites to open the detail screen:

- Big avatar, display name and prettified number
- Chips: **approximate location · carrier · number type**
- **Offline risk analysis** banner (safe / watch out / likely spam / scam
  likely) with human-readable reasoning
- Full info table: number, type, carrier, location, country (+dial code),
  first call, last call, total calls, missed count, total duration, contact status
- **Per-number call history** with direction icons, timestamps and durations
- One-tap actions: call, message, block/unblock, add to contacts, share, copy

### Recents
- Grouped per number with call-count and missed-call badges
- Direction icon, duration, relative time and location on every row
- Long-press menu: call, message, details, block, copy, share
- Search filters names, numbers and T9 patterns

### Dialpad
- Classic keypad with haptic ticks (toggleable)
- **T9 contact suggestions** as you type
- **Live number intel** under the number: carrier · region · type
  (e.g. `Grameenphone · Bangladesh · Mobile`)
- Call, add-to-contacts and long-press-to-clear backspace

### Incoming caller ID screen
- Full-screen gradient call UI with avatar, name, number and
  **location / carrier / risk chips**
- Answer with running call timer, mute & speaker toggles, or decline
- Settings → **Simulate incoming call** demos it with your real recents
  (real incoming calls are handled by the system dialer unless Aurora is
  set as the default phone app)

### Contacts & favorites
- Alphabetical contact list with fast-scroll section index
- Starred contacts appear in the Favorites grid
- Instant search across all tabs

### Blocking & privacy
- Per-app block list (SharedPreferences — no system default-dialer role needed)
- **“Block suspected spam”** toggle actually hides likely-spam rows from Recents
- Blocked numbers are badged and flagged in risk analysis

### Offline number intelligence (no network, ever)
- 200+ country/dial-code detection from `+CC`, `00CC` and national formats
- **Bangladesh operator mapping**: Grameenphone (013/017), Robi (018),
  Banglalink (019), Airtel (016), Teletalk (015)
- Approximate regions: BD landline area codes, India circles, UK areas,
  300+ NANP area codes → state/province
- Number typing: mobile / landline / toll-free / premium-rate / short code /
  international
- Pretty formatting (`01712 345678`, `(415) 555-0123`, `+44 2079 460958`)
- Heuristic risk scoring (premium rate, repeated short calls, etc.)

### Permissions & demo mode
| Permission | Why |
|---|---|
| `READ_CONTACTS` | Search, T9, favorites, name resolution |
| `READ_CALL_LOG` | Recents list and per-number history |
| `CALL_PHONE` | Place real calls from the app |
| `VIBRATE` | Keypad haptics |

Until permissions are granted the app runs in **Demo mode** with polished
sample data (marked with a “Demo data” chip), so it never looks empty.
Grant access any time from Settings → Permissions. All analysis is
**100 % offline** — the app contains no network code at all.

---

## Install

```bash
adb install dist/aurora-phone-v1.0.0.apk
```

or copy the APK to a device and open it (unknown-sources required).

## Build (no Gradle, no Android SDK)

```bash
./build.sh
```

Pipeline:

```
app/res ── aapt2 compile ──► res.flata
app/AndroidManifest.xml + res.flata ── aapt2 link ──► base.apk (+ R.java)
app/src + R.java ── ECJ (Java 6 level) ──► .class files
.class files ── d8 ──► classes.dex ── zip into APK
zipalign (scripts/zipalign.py) ──► aligned.apk
apksigner (v2/v3, keystore/aurora.jks) ──► dist/aurora-phone-v1.0.0.apk
apksigner verify + aapt2 dump badging ──► sanity check
```

### Toolchain (`tools/` is gitignored)

| Tool | Path | Source |
|---|---|---|
| Java runtime + keytool | `tools/jdk4py/…/java-runtime` | `pip download jdk4py==21.0.8.2` (PyPI) |
| aapt2 (linux x64) | `tools/aapt2-bin/aapt2_64` | github.com/VindroidH/prebuilt-binaries-aapt-aapt_64-aapt2-aapt2_64-windows-linux-macosx |
| ECJ compiler | `tools/android-ide/Android IDE/libs/ecj.jar` | github.com/kedargn/IDEonAndroidForAndroidPlatform |
| android.jar (API 34) | `tools/h2apk/tools/android.jar` | github.com/HashShin/H2APK |
| d8 / R8 | `tools/h2apk/tools/d8.jar` | github.com/HashShin/H2APK |
| apksigner | `tools/h2apk/tools/apksigner.jar` | github.com/HashShin/H2APK |

Restore example:

```bash
pip download jdk4py==21.0.8.2 --no-deps -d tools && unzip -q tools/jdk4py*.whl -d tools/jdk4py
git clone --depth 1 https://github.com/VindroidH/prebuilt-binaries-aapt-aapt_64-aapt2-aapt2_64-windows-linux-macosx.git tools/aapt2-repo
unzip -q tools/aapt2-repo/linux/aapt2_64.zip -d tools/aapt2-bin && chmod +x tools/aapt2-bin/aapt2_64
git clone --depth 1 --filter=blob:none --sparse https://github.com/kedargn/IDEonAndroidForAndroidPlatform.git tools/android-ide
git -C tools/android-ide sparse-checkout set "Android IDE/libs"
git clone --depth 1 --filter=blob:none --sparse https://github.com/HashShin/H2APK.git tools/h2apk
git -C tools/h2apk sparse-checkout set tools
```

> Note: the bundled ECJ is 3.6.2, so sources intentionally stick to
> Java 6 syntax (no diamond operator, no lambdas, no string-switch).

## Project layout

```
app/
  AndroidManifest.xml
  res/            layouts, drawables (vectors), values (themes/colors/strings), menu
  src/com/aurora/
    phone/        activities, models
    phone/data/   call log / contacts / sample data / block list
    phone/util/   NumberGeo, RiskAnalyzer, TimeUtils, Actions
    phone/view/   AvatarView (custom gradient avatar)
    phone/adapter Recents, Contacts, Favorites adapters
build.sh          end-to-end build
scripts/zipalign.py
keystore/aurora.jks   demo signing key (alias `aurora`, storepass `android`)
```

## License / notes

- Sample names and numbers in Demo mode are fictional.
- Risk scoring and region lookups are offline heuristics and are labelled
  as approximate inside the app.
