# Google Play — Store Listing & Console Answers

Prepared for **Tertiary Scanner** (`com.tertiaryinfotech.scannerapp`). Production track.

## Store listing

**App name** (≤30): `Tertiary Scanner`

**Short description** (≤80):
`Scan, enhance & OCR documents to PDF — 100% offline. Nothing leaves your device.`

**Full description** (≤4000):
```
Tertiary Scanner turns your phone into a fast, private document scanner. Capture any
document with automatic edge detection and perspective correction, clean it up with
one-tap filters, recognize the text, and export a polished PDF — all completely offline.

WHY TERTIARY SCANNER
• 100% offline — no account, no ads, no tracking. Nothing ever leaves your device.
• Fast, native, and lightweight.

SCAN
• Automatic edge detection and perspective correction.
• Multi-page documents in a single scan.
• Import existing photos from your gallery.

ENHANCE
• One-tap filters: Auto, White Document, Black & White, Denoise, Bright, Sharpen Text,
  and Receipt mode.
• Rotate pages and re-order as needed.

RECOGNIZE TEXT (OCR)
• On-device text recognition extracts the words from your scans.
• Saved text makes every document searchable by its contents.

EXPORT & SHARE
• Export multi-page PDFs (A4, Letter, or fit-to-image) with adjustable quality.
• Save images to Photos, save files via the system file picker, or share anywhere.
• Copy or share recognized text.

ORGANIZE
• A searchable library — find documents by name or by the text inside them.
• Rename, duplicate, and delete.

PRIVATE BY DESIGN
Every step — scanning, enhancement, OCR, and PDF creation — runs on your device.
The app requests no network access and collects no personal data.

Powered by Tertiary Infotech Academy Pte Ltd.
```

**App category:** Productivity
**Tags:** document scanner, PDF, OCR, scan
**Contact email:** angch@tertiaryinfotech.com
**Website:** https://www.tertiaryinfotech.com
**Privacy Policy URL:** (REQUIRED — see PRIVACY_POLICY.md; must be hosted at a public URL)

## Graphic assets (in this folder)
- App icon: `play_store_512.png` (512×512)
- Feature graphic: `feature_graphic_1024x500.png` (1024×500)
- Phone screenshots: `screenshot_1_home.png`, `screenshot_2_settings.png`
  (will add scan / filter / library / detail screenshots from the on-device test)

## Content rating questionnaire (IARC)
- Category: **Utility, Productivity, Communication, or Other**
- Violence / sexual / nudity / profanity / drugs / gambling: **No** to all
- User-generated content / sharing: the app can share user files the user creates → answer
  per actual behavior (no in-app social features, no content from other users).
- Does the app share the user's location: **No**
- Expected rating: **Everyone**

## Data safety form
- **Does your app collect or share any of the required user data types?** → **No**
- Data collected: **None**
- Data shared: **None**
- All processing on-device; the app has no network/internet permission for analytics.
- Is all user data encrypted in transit: N/A (no data leaves the device)
- Do you provide a way for users to request data deletion: N/A (no data collected)

## App content declarations
- **Ads:** No, the app does not contain ads.
- **Target audience:** 18+ (or 13+) — not designed for children; no Families program.
- **Government app:** No
- **Financial features:** No
- **Health:** No
- **App access:** All functionality available without special access / login (no credentials
  needed for review).

## Release
- Track: **Production**
- Artifact: `app/build/outputs/bundle/release/app-release.aab` (versionCode 1, versionName 1.0)
- Play App Signing: **Enroll** (recommended) — Google manages the app signing key; our
  keystore is the upload key.
- Release name: `1.0 (1)`
- Release notes (en-US): `First release of Tertiary Scanner — offline document scanning,
  enhancement, OCR, and PDF export.`

> NOTE: New personal Play developer accounts may be required to run a **closed test with 20
> testers for 14 days** before Production is unlocked. Organization accounts are exempt. If the
> Console blocks Production, we start with Closed testing and promote later.
