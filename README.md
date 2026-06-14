# Tertiary Scanner — Android

A native **Android document scanner**. Scan with edge detection and perspective correction,
enhance with on-device filters, recognize text (OCR), and export to PDF/JPG — **fully offline.
Nothing leaves your device.**

This is the Android port of the iOS *Tertiary Scanner*, rebuilt natively in Kotlin + Jetpack
Compose with the same features and design.

![Platform](https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Offline](https://img.shields.io/badge/100%25-Offline-success)

## Features

- 📷 **Scan** — ML Kit Document Scanner: automatic edge detection, perspective correction,
  multi-page capture, and gallery import.
- 🎨 **Enhance** — 8 one-tap filters (Auto, White, B&W, Denoise, Bright, Sharpen, Receipt,
  Original), applied live; rotate pages 90°.
- 🔤 **OCR** — on-device text recognition (ML Kit); recognized text is saved and **searchable**.
- 📄 **Export** — share or save a multi-page **PDF** (A4 / Letter / Fit-to-image), save **JPGs**
  to Photos, save to **Files** (Storage Access Framework), or share the recognized text.
- 🗂️ **Library** — searchable document list (by name *or* recognized text), rename, duplicate,
  delete.
- 🔒 **Private** — no network access, no analytics, no accounts. All processing is on-device.

## Tech stack

| Area | Choice |
|------|--------|
| Language / UI | Kotlin 2.0, Jetpack Compose, Material 3 |
| Architecture | MVVM, manual DI (`AppContainer`) |
| Capture | `play-services-mlkit-document-scanner` (+ Photo Picker fallback) |
| OCR | `com.google.mlkit:text-recognition` |
| Imaging | `ImageProcessor` — ColorMatrix / LUT / convolution (ports of the iOS Core Image graphs) |
| PDF | `android.graphics.pdf.PdfDocument` |
| Persistence | Room (metadata) + JPEG files under `filesDir/Scans/` (images) |
| Settings | DataStore Preferences |
| Navigation | Navigation Compose |
| Build | Gradle 8.14.3, AGP 8.13.0, JDK 17, compileSdk 36, minSdk 24 |

## Build

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew :app:assembleDebug     # debug APK  -> app/build/outputs/apk/debug/
./gradlew installDebug           # install on a connected device/emulator
./gradlew :app:bundleRelease     # signed AAB -> app/build/outputs/bundle/release/  (needs keystore.properties)
```

Open the folder in **Android Studio** and Run ▶ for the usual workflow.

## Release signing

Release builds read a **gitignored** `keystore.properties` at the repo root:

```properties
RELEASE_STORE_FILE=/absolute/path/to/upload-keystore.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=tertiary-scanner
RELEASE_KEY_PASSWORD=…
```

Keep the keystore **outside** the repo and **backed up** — it is required to publish updates.

## Project layout

```
app/src/main/java/com/tertiaryinfotech/scannerapp/
  model/      FilterType
  data/       Room entities, DAO, AppDatabase
  settings/   SettingsStore (DataStore)
  imaging/    ImageProcessor
  service/    BitmapIo, OcrService, PdfService, ExportService, StorageService, Working*
  vm/         ScannerViewModel, LibraryViewModel, SettingsViewModel
  ui/         AppNav, Capture, screens, components, theme
```

---

Powered by **Tertiary Infotech Academy Pte Ltd**.
