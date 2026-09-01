# AethelionCS

CloudStream 3 / 4 Extension Repository for Turkish Streaming Providers.

## 📌 Supported Providers

### 1. DiziBox (`DiziboxProvider`)
- **Base URL:** `https://www.dizibox.live`
- **Type:** `TvSeries`
- **Language:** Turkish (`tr`)
- **Features:**
  - Full catalog search via archive interface (`/arsiv/?&dizi=`)
  - Single-season and Multi-season episode listing
  - Contextual Season & Episode regex parsing
  - 2-level nested player resolution (`Player King -> Molystream / VidMoly`)
  - Upstream `Vidmoly` extractor integration
  - Subtitle deduplication and callback forwarding

---

## 🏗 Architecture & Source Resolution Pipeline

```text
DiziBox Episode (/adults-2-sezon-3-bolum-izle/)
  ↓
Primary Iframe (/player/king/king.php?v=...)
  ↓ (Player King Wrapper)
Nested Iframe (dbx.molystream.org/embed/{id})
  ↓ (Molystream / VidMoly Host)
com.lagradost.cloudstream3.extractors.Vidmoly
  ↓ (AES Decryption)
HLS Master Stream (master.m3u8) / Native ExoPlayer
```

---

## 📥 Installation

1. Open **CloudStream** on your Android device.
2. Go to **Settings** → **Extensions** → **Add Repository**.
3. Enter the repository details:
   - **Repository Name:** `AethelionCS`
   - **Repository URL:** `https://raw.githubusercontent.com/KorhanWithSunglasses/AethelionCS/master/repo.json`
4. Click **Download** and install the **DiziBox** plugin.

---

## 🛠 Development & Build

### Requirements
- JDK 17 or JDK 21
- Android SDK (compileSdk 34)

### Commands
```bash
# Run unit tests
./gradlew test

# Build plugin and generate plugins.json
./gradlew makePluginsJson

# Clean build directory
./gradlew clean
```

---

## 🗺 Maintenance Map

| Component | Source Selector / Endpoint | Notes |
| :--- | :--- | :--- |
| **Search** | `/arsiv/?&dizi={query}` | Stable archive search endpoint |
| **Catalog** | `/arsiv/` | Full archive DOM index |
| **Series Title** | `h1` | Series page header |
| **Poster** | `img.main-cover` | 200x290 poster thumbnail |
| **Season Tabs** | `a[href*="/dizi/"][href*="-sezon-"]` | Multi-season navigation tabs |
| **Episode Links** | `.season-episode a`, `a[href*="-sezon-"][href*="-bolum-"]` | Episode list rows |
| **Player Wrapper** | `iframe[src*="/player/king/"]` | Primary iframe container |
| **Video Host** | `iframe[src*="molystream.org"]` | Nested VidMoly embed |

---

## ⚠️ Disclaimer & Legal Notice

This extension is developed for educational and personal research purposes. All media content and streams are resolved from public, freely accessible third-party hosters. No DRM, CAPTCHA, or access-control mechanisms are bypassed.

---

## 📄 License

GPL-3.0 License.
