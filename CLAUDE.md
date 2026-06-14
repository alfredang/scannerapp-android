# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**Tertiary Scanner (Android)** — a native Android document scanner (Kotlin / Jetpack Compose /
Material 3, MVVM, fully offline). It is the Android port of the iOS app of the same name
(`../../iOS/scannerapp`). Scan with the ML Kit Document Scanner, enhance with on-device image
filters, OCR with ML Kit Text Recognition, export PDF/JPG to Photos, Files (SAF), or share.
No backend; everything runs on-device.

- Application ID: `com.tertiaryinfotech.scannerapp` · Play listing name: **Tertiary Scanner**
- minSdk 24 · targetSdk/compileSdk 36 · JDK 17 · Gradle 8.14.3 · AGP 8.13.0 · Kotlin 2.0.21

> Keep the Android and iOS apps separate — do not share build files or mix the two projects.

## Build & run

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:bundleRelease          # signed release AAB for Play (needs keystore.properties)
./gradlew installDebug                 # install on the connected device/emulator
```

There is no code generation step (unlike the iOS XcodeGen project) — Gradle discovers sources
automatically.

## Architecture (MVVM)

```
model/      FilterType (8 enhancement filters, stable raw values)
data/       Room — ScanDocumentEntity, ScanPageEntity, DocumentWithPages, ScanDao, AppDatabase
settings/   SettingsStore (DataStore Preferences): pdfQuality, pdfPageSize, defaultFilter
imaging/    ImageProcessor — ColorMatrix/convolution ports of the iOS Core Image filter graphs
service/    BitmapIo, OcrService (ML Kit), PdfService (PdfDocument), ExportService
            (MediaStore/SAF/share), StorageService (files + Room), Working{Document,Page}
vm/         ScannerViewModel (capture→edit→save), LibraryViewModel, SettingsViewModel
ui/         AppNav (Navigation Compose), Capture (ML Kit + Photo Picker fallback),
            Home/Preview/Filter/Export/Library/DocumentDetail/Settings screens, components/
ScannerApplication.kt  — AppContainer (manual DI: db, dao, storage, settings)
```

### Conventions & gotchas
- **Persistence split** mirrors iOS: document/page *metadata* in Room; page *images, thumbnails*
  are JPEG files under `filesDir/Scans/<documentId>/` (`page_<i>_{original,processed,thumb}.jpg`,
  quality 85, thumb max-edge 320). Entities store filenames only.
- **Capture**: `ui/Capture.kt` launches the ML Kit Document Scanner (edge detection, perspective
  correction, multi-page, gallery import). It falls back to the system Photo Picker when Google
  Play services is unavailable (some emulators) — this mirrors the iOS Simulator photo-picker
  fallback.
- **Heavy work off the main thread**: image filtering, OCR, and PDF building run on
  `Dispatchers.Default`/`IO`. `ScannerViewModel` re-renders a page's processed bitmap whenever
  its filter or rotation changes.
- **Filters**: the exact Core Image parameters were ported to ColorMatrix (color ops),
  per-channel LUTs (highlight/shadow, auto-levels), and a 3×3 convolution (sharpen/denoise).
  Add a filter = new `FilterType` case + a branch in `ImageProcessor.apply()`.
- **Editor ViewModel is Activity-scoped** (`viewModel(viewModelStoreOwner = activity)`) so
  Preview/Filter/Export share one `ScannerViewModel` across the nav back stack.

## Release / Play Store

- Signing is read from a **gitignored** `keystore.properties` at the repo root
  (`RELEASE_STORE_FILE/PASSWORD`, `RELEASE_KEY_ALIAS/PASSWORD`). The upload keystore lives
  **outside** the repo at `~/.android-keystores/tertiary-scanner-upload.jks`.
  **Back this keystore up** — losing it (without Play App Signing key reset) means you can no
  longer update the app.
- Build the upload artifact: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
- Play Console first-publish steps (app creation, store listing, content rating, Data safety,
  target-audience, pricing) are web-UI; the Play Developer API can automate later track uploads.
- Data safety: **no data collected, no data shared, all processing on-device.**

## Do not commit
`keystore.properties`, `*.jks`/`*.keystore`, `local.properties`, `/build`, `/.gradle`, `/.idea`.
