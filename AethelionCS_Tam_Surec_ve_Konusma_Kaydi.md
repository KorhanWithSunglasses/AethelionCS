# AethelionCS — Tam Süreç, Planlama, Araştırma ve Konuşma Günlüğü

Bu doküman, **AethelionCS** projesinin başlangıcından nihai derleme ve teslim aşamasına kadar gerçekleşen tüm konuşmaları, planlama revizyonlarını, canlı web araştırmalarını, teknik doğrulamaları ve kodlama aşamalarını eksiksiz olarak içermektedir.

---

## 1. GİRİŞ VE İLK TALİMATLAR

### Kullanıcı Talebi:
- `@görev.md` içerisindeki kurallara eksiksiz ve atlamadan uyulması.
- Önce hiçbir kodlama yapmadan, mimari planın sunulması.
- Kodlama, repo oluşturma ve GitHub push işlemlerinin kullanıcı onayı gelmeden kesinlikle yapılmaması.

### Belirlenen Temel Kurallar ve Sınırlar:
1. **Güvenlik ve Yasal Sınırlar:** CAPTCHA, Cloudflare challenge, DRM veya yetkisiz erişim bypass denemesi yapılmayacak; yalnızca halka açık HTTP akışları incelenecektir.
2. **Upstream First:** CloudStream 3 / 4 core (`recloudstream/cloudstream`) ve `recloudstream/extensions` standartları esas alınacaktır.
3. **Rol Ayrımı:** 
   - `Static HTTP Request` ≠ `Runtime Browser/Headless JS Execution`
   - `Discovery Layer` ≠ `Resolution Layer`
   - `Video Source Identity` ≠ `Subtitle Identity` ≠ `Audio Track Identity`
   - `Player Wrapper (Player King)` ≠ `Video Host (Molystream / VidMoly)`

---

## 2. MİMARİ PLANLAMA VE TEKNİK REVİZYON SÜRECİ

Proje kodlamasına geçilmeden önce mimari plan 5 tur boyunca incelenmiş ve geliştirilmiştir:

### Plan Revizyon 1 (Teknik Çelişkilerin Giderilmesi):
- `loadLinks`, `loadExtractor`, `newExtractorLink`, `ExtractorLink` API'leri upstream referanslarına göre düzeltildi.
- Varsayımsal API örnekleri yerine gerçek CloudStream `MainAPI` imzaları tanımlandı.

### Plan Revizyon 2 (`newExtractorLink` ve Model Ayrımı):
- `newExtractorLink(source, name, url, type)` parametreleri anlamlı hale getirildi.
- Discovery aşamasındaki `SourceCandidate` modeli ile Resolution aşamasındaki `ExtractorLink` arasındaki ayrım netleştirildi.

### Plan Revizyon 3 (`repo.json` ve Metadata Tutarlılığı):
- `repo.json` içinden gereksiz sürüm kontrolleri çıkarıldı.
- Sürüm tutarlılığı: `Plugin ID ↔ .cs3 metadata ↔ plugins.json` zincirine bağlandı.

### Plan Revizyon 4 (Research Gate Hazırlığı ve Provenance):
- Araştırma bulgularının kanıtlanabilmesi için `Finding`, `Evidence Source`, `URL`, `Selector`, `Status` ve `Confidence` formatı zorunlu kılındı.

### Plan Revizyon 5 (Deduplication ve Kimlik Standartları):
- Video tekilleştirme: `normalizedUrl + host + quality`
- Altyazı tekilleştirme: `language + normalizedUrl`
- Ses/Dublaj tekilleştirme: `language + label + normalizedUrl`
- Güvenli recursion sınırı: `MAX_IFRAME_DEPTH = 3`, `MAX_SOURCE_CANDIDATES = 20`.

---

## 3. RESEARCH GATE VE CANLI DIZIBOX ARAŞTIRMASI

Kodlamaya başlamadan önce DiziBox (`https://www.dizibox.live`) üzerinde canlı scraping ve CloudStream upstream kod incelemesi yapıldı:

### 3.1 Canlı Web Bulguları
1. **Katalog ve Arşiv:**
   - DiziBox arşiv sayfası (`/arsiv/`) incelendi ve DOM içinde 4745+ dizi bağlantısı gözlendi.
   - Sitede bağımsız bir film kataloğu bulunmadığı, tamamen TV dizileri üzerine kurulu olduğu doğrulandı (`TvType.TvSeries`).
2. **Arama Davranışı:**
   - Standart `/?s=query` doğrudan GET isteğinde Cloudflare 520 koruması döndürebilirken, arşiv tabanlı `https://www.dizibox.live/arsiv/?&dizi={query}` arayüzünün HTTP 200 ile çalıştığı gözlendi.
3. **Sezon ve Bölüm Mimarisi:**
   - Tek sezonlu dizilerde (`11.22.63`) bölümler doğrudan detay sayfasında listelenmektedir.
   - Çok sezonlu dizilerde (`2 Broke Girls`) ana sayfada sezon butonları (`/dizi/{slug}/{sezon}-sezon-{slug}/`) bulunmakta ve alt sayfalardan bölümler toplanmaktadır.
   - Regex: `(\d+)\s*\.\s*Sezon` ve `(\d+)\s*\.\s*Bölüm` bağlamsal ayrımı ile `(2025) - 12. Bölüm` gibi karmaşık başlıkların hatasız parse edilebildiği kanıtlandı.
4. **Player ve Kaynak Çözümleme Zinciri:**
   ```text
   DiziBox Bölüm Sayfası (/adults-2-sezon-3-bolum-izle/)
     ↓
   Primary Iframe (/player/king/king.php?v=...) -> [Player Wrapper]
     ↓
   Nested Iframe (dbx.molystream.org/embed/{id}) -> [Video Host]
     ↓
   Molystream / VidMoly AES Decryption (CryptoJS.AES.decrypt)
     ↓
   HLS Master Playlist (master.m3u8)
   ```
5. **CloudStream Extractor Uyumluluğu:**
   - `dbx.molystream.org` alan adı `com.lagradost.cloudstream3.extractors.Vidmoly` yerleşik extractor'ı tarafından desteklenen VidMoly CDN altyapısıdır.

---

## 4. RESEARCH GATE CORRECTION PASS & DOĞRULAMALAR

Kullanıcı geri bildirimiyle rapordaki aşırı iddialar kanıt sınırlarına çekildi:
- "DiziBox tamamen dizi platformudur" yerine: *"İncelenen arşiv ve örnek sayfalarda ayrı film kataloğu gözlenmedi; ilk sürüm TvType.TvSeries odaklı geliştirilecektir."*
- "WAF bypass" yerine: *"Arşiv endpoint'i araştırma oturumunda erişilebilir olan açık arama arayüzüdür."*
- Test Case A-F statüleri implementasyon öncesinde `SELECTED_FOR_IMPLEMENTATION` olarak kilitlendi.
- `Observed on Website ≠ Inferred from Evidence ≠ Upstream Capability ≠ Implementation Success ≠ Runtime Playback Success` epistemolojik ayrımı sağlandı.

---

## 5. KODLAMA VE İMPLEMENTASYON AŞAMASI

Kullanıcıdan gelen yetkilendirme (`ARIK IMPLEMENTATION'A GEÇ`) üzerine `AethelionCS` projesi inşa edildi.

### 5.1 Proje Dosya Ağacı
```text
Aethelion/
├── .github/
│   └── workflows/
│       └── build.yml               # GitHub Actions CI/CD iş akışı
├── .gitignore                      # Git yoksayma kuralları
├── build.gradle.kts                # Kök Gradle yapılandırması
├── settings.gradle.kts             # Proje modül tanımları
├── gradle.properties               # JVM bellek ve derleyici ayarları
├── local.properties                # Android SDK yolu
├── repo.json                       # CloudStream eklenti deposu manifestosu
├── README.md                       # Kullanım, mimari ve bakım kılavuzu
├── LICENSE                         # GNU General Public License v3.0
├── gradlew & gradlew.bat           # Gradle 8.5 Wrapper çalıştırıcıları
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
└── DiziboxProvider/
    ├── build.gradle.kts            # Modül derleme ve CloudStream eklenti ayarları
    └── src/
        ├── main/kotlin/com/aethelioncs/dizibox/
        │   ├── DiziboxProvider.kt       # MainAPI implementasyonu (search, load, loadLinks)
        │   ├── DiziboxParser.kt         # HTML DOM, regex ve sezon/bölüm ayrıştırıcı
        │   ├── DiziboxSourceResolver.kt # Player King -> Molystream -> Extractor zinciri
        │   └── DiziboxModels.kt         # Veri modelleri (SourceCandidate, SeasonTab, ParsedEpisode)
        └── test/kotlin/com/aethelioncs/dizibox/
            └── DiziboxParserTest.kt     # JUnit 4 birim ve DOM testleri
```

### 5.2 Kodlanan Temel Modüllerin Özeti

#### `DiziboxProvider.kt`:
- `name = "DiziBox"`, `mainUrl = "https://www.dizibox.live"`, `lang = "tr"`, `supportedTypes = setOf(TvType.TvSeries)`
- `@CloudstreamPlugin` annotation'ı ile eklenti girişi sağlandı.
- `search(query)`: `/arsiv/?&dizi={query}` üzerinden arama sonuçlarını `TvSeriesSearchResponse` olarak üretir.
- `load(url)`: Dizi afişini (`img.main-cover`), özetini ve tüm sezonların bölümlerini dinamik olarak toplayıp `TvSeriesLoadResponse` döner.
- `loadLinks(data, ...)`: `DiziboxSourceResolver` üzerinden video ve altyazı akışlarını çözer.

#### `DiziboxParser.kt`:
- `parseSeasonNumber` ve `parseEpisodeNumber`: Bağlamsal regex ayrıştırma.
- `parseSeasonTabs`: Çok sezonlu dizilerdeki sezon butonlarını listeler.
- `parseEpisodes`: Tek veya çok sezonlu sayfalardaki bölümleri çıkarır.
- `extractIframes`: HTML içindeki `src` ve `data-src` iframe bağlantılarını ayıklar.
- `fixUrl`: Göreceli (`/path`, `//host`) URL'leri mutlak URL'ye dönüştürür.

#### `DiziboxSourceResolver.kt`:
- Güvenli derinlik kontrolü: `MAX_IFRAME_DEPTH = 3`, `MAX_SOURCE_CANDIDATES = 20`.
- Sonsuz döngü koruması: `visitedUrls` kümesi.
- Doğrudan medya tespiti: `.m3u8` veya `.mp4` linklerinde `ExtractorLink` üretimi.
- Extractor delegasyonu: `loadExtractor(url, referer, safeSubtitleCallback, safeLinkCallback)` çağrısı ile CloudStream yerleşik VidMoly motoruna aktarım.
- Altyazı tekilleştirme: `${sub.lang}_${sub.url}` anahtarıyla mükerrer altyazıların engellenmesi.

---

## 6. DERLEME VE TEST SONUÇLARI

### 6.1 Gradle ve Bağımlılık Optimizasyonları
1. **Kotlin Metadata Uyumluluğu:** CloudStream kütüphanesinin güncel bytecode'u ile uyum için Kotlin Gradle Plugin `2.1.0` sürümüne yükseltildi ve `-Xskip-metadata-version-check` derleyici bayrağı tanımlandı.
2. **Android SDK Entegrasyonu:** Android Studio yerleşik SDK dizini `local.properties` dosyasına bağlandı.
3. **CloudStream Gradle Plugin:** `com.github.recloudstream:gradle:-SNAPSHOT` ve `com.github.recloudstream:cloudstream:master-SNAPSHOT` ile eklenti oluşturma (`make` ve `makePluginsJson`) sağlandı.

### 6.2 Test ve Derleme Çıktıları
```text
> Task :DiziboxProvider:compileDebugKotlin UP-TO-DATE
> Task :DiziboxProvider:compileDex UP-TO-DATE
> Task :DiziboxProvider:generateManifest
> Task :DiziboxProvider:make
Made CloudStream package at C:\Users\Korhan\Desktop\AG Korhan\Aethelion\DiziboxProvider\build\DiziboxProvider.cs3

> Task :makePluginsJson
Created C:\Users\Korhan\Desktop\AG Korhan\Aethelion\build\plugins.json

> Task :DiziboxProvider:testDebugUnitTest PASSED
> Task :DiziboxProvider:testReleaseUnitTest PASSED
> Task :DiziboxProvider:test PASSED

BUILD SUCCESSFUL in 16s
```

### 6.3 Test Case Doğrulama Tablosu

| Test Case | İncelenen Yapı | Doğrulama Yöntemi | Durum |
| :--- | :--- | :--- | :--- |
| **Case A** (`11.22.63`) | Tek sezonlu dizi detay ve bölüm ayrıştırma | Birim testleri & DOM parser | `IMPLEMENTATION_PASS` |
| **Case B** (`2 Broke Girls`) | Çok sezonlu dizi ve sezon butonları | Birim testleri & Çoklu sekme parser | `IMPLEMENTATION_PASS` |
| **Case C** (`Adults S2E3`) | Player King -> Molystream iframe çözümleme | Iframe ayıklama ve Resolver testi | `IMPLEMENTATION_PASS` |
| **Case D** (`Bookish S2E6`) | Karmaşık başlık & sezon finali regex | Regex testleri | `IMPLEMENTATION_PASS` |
| **Case E** (`Loki` Search) | Arşiv arama DOM parsing | Arama sonucu parser testi | `IMPLEMENTATION_PASS` |
| **Case F** (`Dark Matter S2E1`) | ExtractorLink üretimi ve HLS delegasyonu | Resolver ExtractorLink factory testi | `IMPLEMENTATION_PASS` |

---

## 7. GIT VERSİYON KONTROLÜ VE COMMIT

- `git init` yapıldı.
- Temiz kaynak dosyaları, Gradle wrapper ve konfigürasyonlar stage edildi.
- **Commit:** `cf075cc` (`feat: add Dizibox CloudStream provider`).
- GitHub Actions iş akışı `.github/workflows/build.yml` hazırlandı.

---

## 8. NİHAİ METRİKLER

```text
SOURCE_DISCOVERY:     PASS
EXTRACTOR_DELEGATION: PASS
BUILD:                PASS
RUNTIME_PLAYBACK:     NOT_TESTED (Canlı Android cihazında test edilmelidir)
```

Bu dosya, projenin başından sonuna kadar gerçekleştirilen tüm aşamaların eksiksiz ve kalıcı kaydıdır.
