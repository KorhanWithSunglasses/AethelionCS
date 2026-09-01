# AethelionCS — Autonomous CloudStream Extension Development Task

## 0. SENİN GÖREVİN

Sen bu projenin kıdemli Android/Kotlin, CloudStream extension, web scraping/parsing, video-source resolution, GitHub ve CI/CD geliştiricisisin.

Ben teknik implementasyonu yapmayacağım.

Sen:

* araştırmayı,
* güncel dokümantasyonu okumayı,
* mevcut GitHub projelerini incelemeyi,
* mimariyi belirlemeyi,
* kodlamayı,
* refactor işlemlerini,
* testleri,
* build işlemlerini,
* hata çözümünü,
* GitHub repository oluşturmayı,
* commit/push işlemlerini,
* CI/CD kurulumunu,
* plugin repository metadata üretimini,
* README/dokümantasyonu

otonom şekilde yapacaksın.

Ben GitHub hesabıma erişim izni verdim. GitHub tarafında gereken repository/branch/commit/push işlemlerini kendin yap.

Küçük teknik kararlar için benden onay isteme. Güncel upstream kaynaklara bakarak en doğru çözümü kendin seç.

Ancak erişim kontrolü, DRM, CAPTCHA, anti-bot güvenlik mekanizmaları veya yetkisiz erişim konusunda bypass geliştirme.

---

# 1. PROJE

Proje adı:

AethelionCS

İlk milestone yalnızca:

DiziBox

Base URL:

https://www.dizibox.live

İleride başka provider'lar eklenebilecek şekilde mimari kur, ancak ilk sürümde yalnızca DiziBox üzerinde çalış.

---

# 2. ANA HEDEF

CloudStream kullanıcısı AethelionCS repository/plugin'ini eklediğinde normal web erişimi kapsamında DiziBox üzerindeki içerikleri CloudStream arayüzünden keşfedebilmeli ve oynatabilmeli.

Temel akış:

DiziBox
↓
Search
↓
Movie / TV Series
↓
Detail
↓
Season
↓
Episode
↓
Player / Sources
↓
Source Resolution
↓
Existing CloudStream Extractor veya Direct Media
↓
ExtractorLink
↓
CloudStream Player

Amaç yalnızca ilk iframe'i bulmak değildir.

Player/source sisteminin tamamını analiz edip mevcut source seçeneklerini mümkün olduğunca eksiksiz şekilde CloudStream'e aktarmalısın.

---

# 3. KRİTİK KURAL — ÖNCE ARAŞTIR, SONRA KODLA

Kodlamaya başlamadan önce güncel upstream kaynaklarını incele.

Eski bloglardan, eski StackOverflow cevaplarından, eski CloudStream extension'larından veya eski tutorial'lardan API imzası kopyalama.

Önce resmi ve aktif upstream kaynaklarından doğrula.

Özellikle:

https://github.com/recloudstream/cloudstream

https://github.com/recloudstream/extensions

https://recloudstream.github.io/csdocs/devs/gettingstarted/

incele.

Ayrıca GitHub üzerinde aktif ve güncel CloudStream extension'larını araştır.

Özellikle aşağıdaki konularda örnek implementasyon bul:

* MainAPI
* SearchResponse
* MovieSearchResponse
* TvSeriesSearchResponse
* MovieLoadResponse
* TvSeriesLoadResponse
* Episode
* loadLinks
* loadExtractor
* ExtractorApi
* ExtractorLink
* newExtractorLink
* ExtractorLinkType
* SubtitleFile
* AudioFile
* repository metadata
* plugins.json
* repo.json
* GitHub Actions
* Gradle yapılandırması.

Referans alınan kodları birebir kopyalama. Güncel ve lisans uyumlu tasarımı öğrenip kendi implementasyonunu yap.

---

# 4. GÜNCEL SÜRÜM KURALI

Versiyonları tahmin etme.

Build başlamadan önce güncel CloudStream upstream kaynaklarından doğrula:

* Kotlin
* Gradle
* Android Gradle Plugin
* compileSdk
* targetSdk
* Java/Kotlin target
* CloudStream library
* CloudStream Gradle plugin
* NiceHttp
* Jsoup
* diğer gerekli dependency'ler.

"En yeni sürüm" olduğu için körlemesine dependency yükseltme.

Öncelik:

1. resmi CloudStream upstream
2. resmi CloudStream extensions
3. aktif güncel extension'lar
4. dependency compatibility
5. Kotlin/Gradle/Android uyumluluğu.

Bir API'nin güncel imzasından emin değilsen doğrudan upstream source kodunu aç ve doğrula.

---

# 5. DEPRECATED API KULLANMA

Compiler warning'lerini önemse.

Bir API deprecated ise:

1. neden deprecated olduğunu araştır,
2. yerine gelen güncel API'yi bul,
3. güncel API'yi kullan.

Özellikle eski:

ExtractorLink constructor'ları,
eski loadLinks signature'ları,
eski CloudStream helper'ları,
eski Gradle yapılandırmaları

kullanma.

Örneğin direct media source üretirken güncel upstream'deki `newExtractorLink(...)` yaklaşımını esas al.

Method signature'larını tahmin etme.

---

# 6. DİZİBOX ANALİZİ

Gerçek DiziBox HTML yapısını araştır.

Sadece ana sayfayı değil, mümkün olduğunca farklı gerçek içerikleri incele.

En azından:

* bir film
* bir tek sezonlu dizi
* birden fazla sezonlu dizi
* farklı episode sayısına sahip dizi
* birden fazla video source'u olan içerik
* subtitle bulunan içerik, bulunabiliyorsa
* source yapısı farklı olan içerik

üzerinde inceleme yap.

Şunları tespit et:

### Search

* search endpoint
* query parametreleri
* pagination
* result cards
* title selector
* poster selector
* year
* type
* detail URL

### Detail

* movie/series ayrımı
* title
* original title
* poster
* backdrop
* description
* year
* rating
* genres
* country
* tags
* metadata

### TV Series

* season selector
* season name
* season number
* episode list
* episode number
* episode name
* episode URL
* episode thumbnail

### Player

Çok kritik:

Sayfa yüklenince gerçekten video URL'si HTML içinde var mı?

Yoksa:

DiziBox page
→ source selector
→ AJAX
→ iframe
→ video host
→ player
→ video

gibi bir yapı mı var?

Şunları araştır:

* iframe
* embed URL
* source buttons
* data attributes
* JavaScript variables
* script tags
* AJAX endpoints
* JSON
* lazy-loaded values
* encoded values
* player configuration
* video URL
* subtitle URL
* audio track
* quality
* referer
* required headers.

---

# 7. PLAYER / SOURCE RESOLUTION — EN KRİTİK KATMAN

Provider yalnızca HTML'den ilk video URL'sini çekmeye çalışma.

DiziBox üzerindeki source sistemini tamamen çözümle.

Örneğin:

DiziBox page
↓
Source 1
↓
iframe URL
↓
VidMoly
↓
Extractor
↓
M3U8

ve:

DiziBox page
↓
Source 2
↓
AJAX endpoint
↓
iframe
↓
Vidoza
↓
Extractor

ve:

DiziBox page
↓
Source 3
↓
direct .m3u8

gibi yapıların tamamını destekleyebilecek mimari kur.

---

# 8. EXISTING CLOUDSTREAM EXTRACTOR'LARINI ÖNCELİKLENDİR

DiziBox source'u şunlardan birine gidiyorsa önce CloudStream upstream'de mevcut ExtractorApi desteğini kontrol et:

* VidMoly
* Vidoza
* Dood / Doodstream
* OK.ru
* Mail.ru
* JWPlayer tabanlı public player
* diğer popüler video hostları.

Listeyi sabit kabul etme.

Gerçek DiziBox source hostlarını keşfet.

Sonra CloudStream upstream'de bunlara ait mevcut extractor olup olmadığını kontrol et.

Existing extractor varsa:

KENDİ EXTRACTOR'INI YAZMA.

Güncel:

`loadExtractor(url, referer, subtitleCallback, callback)`

mekanizmasını kullan.

Ama method signature'ını güncel upstream'den doğrula.

Mantıksal yapı:

DiziBox source
→ existing CloudStream extractor
→ extractor resolves stream
→ ExtractorLink
→ CloudStream player

olmalı.

Bu şekilde DiziBox provider, video hostlarının bütün extraction logic'ini tekrar implement etmek zorunda kalmaz.

---

# 9. DIRECT MEDIA FALLBACK

Eğer DiziBox veya iframe doğrudan media URL veriyorsa:

* `.m3u8`
* `.mp4`
* `.mpd`
* CloudStream'in desteklediği diğer direct media formatları

için mevcut güncel `newExtractorLink(...)` API'sini kullan.

`ExtractorLink` eski constructor'larını kullanma.

Gerçek medya tipine göre güncel:

`ExtractorLinkType`

değerini kullan.

Örneğin:

* M3U8
* DASH
* VIDEO

gibi mevcut güncel upstream enum/type seçeneklerini source kodundan doğrula.

URL extension'ına körü körüne güvenme.

Player configuration, response metadata veya başka güvenilir bilgi gerçek formatı daha doğru belirliyorsa onu tercih et.

---

# 10. REFERER / HEADERS

Bir iframe/video source normal browser akışında referer gerektiriyorsa güncel CloudStream API'sinin desteklediği yöntemle `referer` bilgisini source'a aktar.

Gerekliyse desteklenen HTTP headers'ı aktar.

Ancak:

* cookie/token/password loglama
* session secret commit etme
* kullanıcıya ait hassas authentication bilgisi yazma

yapma.

---

# 11. SOURCE ROUTER TASARIMI

Source resolution mantığını provider kodunda birbirine karıştırma.

İdeal mantıksal akış:

resolveDiziboxSources(...)
↓
List<SourceCandidate>
↓
for each candidate
↓
is supported by CloudStream extractor?
├── yes → loadExtractor(...)
└── no
↓
direct media?
├── yes → newExtractorLink(...)
└── no → safe failure / log

Gereksiz abstraction yaratma.

Fakat source parser ile playback resolution birbirine fazla karışıyorsa küçük internal model oluşturabilirsin.

Örneğin:

SourceCandidate {
url
name
referer
headers
quality?
metadata?
}

Ancak sadece gerçekten gerekli olduğunda oluştur.

---

# 12. SOURCE DEDUPLICATION

Bir source birden fazla yerde keşfedilirse aynı URL'yi duplicate callback olarak gönderme.

URL normalization yap.

Bununla birlikte farklı quality veya farklı playback özellikleri gerçekten farklıysa bunları yanlışlıkla tek source altında birleştirme.

---

# 13. SOURCE FAILURE ISOLATION

Bir source başarısız olursa:

DİĞER SOURCE'LARIN ÇALIŞMASINI ENGELLEME.

Örneğin:

Source 1 → başarısız
Source 2 → başarılı
Source 3 → başarılı

olduğunda CloudStream Source 2 ve Source 3'ü göstermeye devam etmeli.

Bir source için sahte/fake URL üretme.

---

# 14. QUALITY DETECTION

Mümkünse:

144p
240p
360p
480p
720p
1080p
1440p
2160p

gibi bilgileri tespit et.

Ancak yalnızca güvenilir bilgi varsa kalite ata.

Şu kaynaklardan gelebilir:

* filename
* query parameter
* JSON metadata
* m3u8 variant
* player config
* extractor metadata.

Bilinmiyorsa:

Unknown

kullan.

Quality bilgisini tahmin etme.

---

# 15. SUBTITLE

DiziBox veya video extractor:

* Türkçe
* İngilizce
* Arapça
* vb.

subtitle verisi sağlıyorsa CloudStream'in güncel `SubtitleFile` API'siyle `subtitleCallback` üzerinden aktar.

Mevcut extractor zaten subtitle callback sağlıyorsa duplicate subtitle üretme.

Subtitle URL'sini gereksiz yere proxy etme.

---

# 16. AUDIO / DUBBING

Player/source metadata içerisinde ayrı audio track veya dil bilgisi bulunuyorsa CloudStream'in güncel audio mekanizması destekliyorsa kullan.

Örneğin:

* Turkish
* Original
* English
* Arabic

gibi track'leri metadata'dan güvenilir şekilde çıkar.

Site sadece "Dublaj" şeklinde bir ifade veriyorsa bunun gerçekten ayrı audio track mi yoksa ayrı source mu olduğunu analiz et.

Varsayım yapma.

---

# 17. MOVIE / TV SERIES TYPE TESPİTİ

Search response sırasında film/dizi ayrımı çok önemli.

Öncelik sırası:

1. açık DOM/type metadata
2. kategori/badge
3. URL pattern
4. gerekiyorsa detail page verification.

Örneğin URL:

`/dizi/...`

gibi bir pattern veriyorsa kullanılabilir.

Ancak sadece URL pattern'e bağlanma.

CloudStream doğru şekilde:

MovieSearchResponse

veya:

TvSeriesSearchResponse

üretmeli.

Yanlış type üreterek detail/load ekranının çökmesine izin verme.

---

# 18. SEASON PARSING

Aşağıdaki formatları mümkün olduğunca destekle:

* `Sezon 1`
* `1. Sezon`
* `Season 1`
* `S01`
* `S1`
* `Sezon 0`
* özel sezon adları.

Ayrı yardımcı fonksiyon:

`parseSeasonNumber(text: String): Int?`

oluştur.

Special/unknown season durumunda güvenli null/fallback yaklaşımı kullan.

---

# 19. EPISODE PARSING

Ayrı yardımcı:

`parseEpisodeNumber(text: String): Int?`

oluştur.

Mümkün olduğunca:

* `Bölüm 1`
* `1. Bölüm`
* `Episode 1`
* `E01`
* `E1`
* `Bölüm 12`

formatlarını tanı.

Parser yalnızca string içerisindeki ilk sayıyı alma şeklinde çalışmamalı.

Örnek:

`The Last of Us (2025) - 12. Bölüm`

sonucunda:

episode = 12

olmalı.

2025 alınmamalı.

Regex parser bağlamsal ve güvenli olsun.

---

# 20. ÖZEL BÖLÜMLER

Aşağıdaki gibi durumları düşün:

* Özel Bölüm
* Special
* OVA
* Christmas Special
* Season 0
* Bonus
* Unknown episode number.

Gerçek veride karşılaşırsan CloudStream'in episode modeline uygun şekilde en güvenli şekilde temsil et.

Bir değeri zorla 0 veya 1 yapma.

---

# 21. EPISODE SORTING

Bölüm listesi CloudStream'de mantıklı sırada görünmeli.

Sort için:

1. sezon numarası
2. episode number
3. gerekiyorsa fallback title

kullan.

Site episode'ları ters sırada veriyorsa uygun şekilde normalize et.

Ancak özel bölümlerin sırasını bozma.

---

# 22. SEARCH

Search performansını makul tut.

Arama sonucunda mümkünse:

* title
* URL
* poster
* type
* year

doldur.

Poster URL'lerini absolute URL'ye çevir.

HTML entity'leri düzelt.

Relative URL'leri normalize et.

Pagination yalnızca site tarafından destekleniyorsa uygula.

---

# 23. DETAIL PAGE PARSER

Film:

MovieLoadResponse

Dizi:

TvSeriesLoadResponse

üzerinden güncel API ile yüklenmeli.

Metadata mümkün olduğunca doldur:

* title
* poster
* plot
* year
* rating
* tags
* genre
* country.

CloudStream modelinin desteklemediği gereksiz data için özel mekanizma üretme.

---

# 24. HTML PARSER TASARIMI

Parser'ı mümkün olduğunca küçük fonksiyonlara ayır.

Örneğin:

* parseSearchResult()
* parseMoviePage()
* parseSeriesPage()
* parseSeasons()
* parseEpisodes()
* parsePlayerSources()
* parseSubtitleSources()
* resolveSource()

Ama her şeyi gereksiz sınıflara bölme.

Kod okunabilir ve bakım yapılabilir olsun.

---

# 25. ROBUST PARSING / FALLBACK

Tek bir CSS selector'a bağımlı kalma.

Örneğin:

primary selector
→ secondary selector
→ script/JSON fallback

gibi kontrollü fallback kullan.

Ancak yüzlerce alternatif selector yazıp kodu kırılgan hale getirme.

Site değiştiğinde mümkün olduğunca tek bir parser fonksiyonunun güncellenmesi yeterli olsun.

---

# 26. JAVASCRIPT / PACKED CODE

DiziBox veya host player configuration JavaScript içinde data tutuyorsa:

Önce CloudStream upstream'deki mevcut utilities'leri araştır.

Örneğin upstream'de ilgili JS unpack/decode utility varsa onu kullan.

Kendi JavaScript evaluator/unpacker sistemini gereksiz yere yazma.

Encoded URL / escaped JSON / HTML entities gibi normal parsing ihtiyaçlarını doğru şekilde çöz.

---

# 27. AJAX / DYNAMIC PLAYER

Player source'ları AJAX endpoint'lerinden geliyor olabilir.

Böyle bir endpoint varsa:

* request method
* URL
* parameters
* headers
* response type
* response parsing

ayrıntılı şekilde analiz et.

Public page flow içinde normal browser request'i olarak yapılabilen işlemleri CloudStream request sistemiyle uygula.

Endpoint'in HTML source'ta olmadığı, JS tarafından oluşturulduğu durumda ilgili script'i analiz et.

---

# 28. CLOUDFLARE / ANTI-BOT

DiziBox zaman zaman Cloudflare veya benzeri HTTP challenge döndürebilir.

Önce:

* güncel CloudStream HTTP client
* NiceHttp
* normal cookie/session desteği
* standart browser-like headers
* redirect handling

gibi mevcut ve desteklenen mekanizmaları araştır.

Normal public erişim için gerekiyorsa uygun request/session yönetimini kullan.

Ancak:

* Cloudflare challenge bypass
* anti-bot bypass
* CAPTCHA çözme/bypass
* güvenlik mekanizmasını etkisizleştirme
* exploit
* unauthorized access

geliştirme.

Eğer site normal public request'i challenge ile engelliyorsa:

* logla,
* graceful failure yap,
* diğer erişilebilir source'ları çalıştır,
* README/troubleshooting kısmında bilinen sınırlama olarak belirt.

---

# 29. NETWORK ERROR HANDLING

Şunları güvenli şekilde ele al:

* timeout
* connection failure
* 403
* 404
* 429
* 5xx
* malformed HTML
* null selector
* missing iframe
* invalid source
* extractor failure.

Bir request başarısız olduğunda extension'ın tamamen çökmesini önle.

---

# 30. LOGGING

Debug sırasında anlamlı log kullan:

* search started
* result count
* detail type
* season count
* episode count
* player source count
* source host
* extractor chosen
* direct media detected.

Fakat loglara asla:

* password
* access token
* private cookie
* session secret
* kişisel authentication bilgisi

basma.

---

# 31. HTTP REQUEST STANDARDIZATION

Güncel CloudStream request altyapısını kullan.

Mümkünse ortak HTTP/helper katmanı oluştur.

Normal request özelliklerini düzgün ayarla:

* User-Agent
* Referer
* timeout
* redirects
* encoding
* gerekli public headers.

Her parser içinde rastgele HTTP client üretme.

---

# 32. TEST STRATEJİSİ

Aşağıdaki senaryoları gerçek içeriklerle test et:

### Test A

Bir film:

search
→ open
→ details
→ source resolution
→ playback link

### Test B

Tek sezonlu dizi:

search
→ open
→ season
→ episode list
→ episode
→ source
→ playback

### Test C

Çok sezonlu dizi:

search
→ open
→ Season 1
→ Episode
→ Season 2
→ Episode

### Test D

Birden fazla source:

Source 1
Source 2
Source 3

hepsinin keşfedildiğini doğrula.

### Test E

Bir source başarısız:

Source 1 fail
Source 2 success

Source 2'nin hâlâ çalıştığını doğrula.

### Test F

Subtitle:

subtitle bulunduğunda CloudStream'e ulaştığını doğrula.

### Test G

Quality:

mümkünse 360p/720p/1080p gibi kalite bilgilerinin doğru aktarıldığını doğrula.

---

# 33. FIXTURE TESTLER

Mümkünse parser seviyesinde fixture testleri oluştur.

Ayrı fixture'lar:

* search HTML
* movie HTML
* series HTML
* season/episode HTML
* player HTML
* AJAX JSON
* source configuration.

Test et:

* search parsing
* movie parsing
* TV parsing
* episode parsing
* source parsing
* subtitle parsing.

Gerçek site değişikliklerinde fixture'lar maintenance için referans olarak kullanılmalı.

Kişisel cookie/session/token gibi bilgileri fixture içine koyma.

---

# 34. PROJECT STRUCTURE

Güncel CloudStream extension template yapısını esas al.

Muhtemel yapı:

AethelionCS/
README.md
LICENSE
build.gradle.kts
settings.gradle.kts
gradle.properties
repo.json / güncel equivalent
plugins.json / güncel equivalent
gradle/
.github/
workflows/
DiziboxProvider/
build.gradle.kts
src/
main/
kotlin/
...
DiziboxProvider.kt

Bu yalnızca örnektir.

Gerçek repository oluşturulurken güncel CloudStream extensions template'i esas alın.

Package:

`com.aethelioncs.dizibox`

uygun ise kullanılabilir.

Ancak başlamadan önce mevcut naming convention'ı doğrula.

---

# 35. PROVIDER

Provider sınıfı güncel:

`MainAPI`

üzerinden geliştir.

Örneğin mantıksal olarak:

`DiziboxProvider`

Provider adı:

`DiziBox`

olmalı.

URL:

`https://www.dizibox.live`

Ancak URL'nin runtime'da değişmesi veya redirect olması durumunda hardcoded assumption'ları kontrol et.

---

# 36. CLOUDSTREAM API KONTROLÜ

Implementasyondan önce güncel upstream source kodundan kesin olarak kontrol et:

* MainAPI
* SearchResponse
* MovieSearchResponse
* TvSeriesSearchResponse
* MovieLoadResponse
* TvSeriesLoadResponse
* Episode
* loadLinks
* loadExtractor
* ExtractorApi
* ExtractorLink
* newExtractorLink
* ExtractorLinkType
* Qualities
* SubtitleFile
* AudioFile
* güncel HTTP/NiceHttp API.

Metot imzasını tahmin etme.

---

# 37. EXISTING EXTRACTOR SEARCH

Bir video hostu bulunduğunda GitHub upstream içinde gerçekten mevcut extractor olup olmadığını kontrol et.

Örneğin VidMoly bulunduysa:

* extractor class
* package
* supportedHosts
* current implementation
* last relevant update

incele.

Mümkünse direct implementation yerine `loadExtractor` kullan.

---

# 38. FUTURE-PROOFING

İlk provider DiziBox olsa da repository yapısını gelecekte şöyle genişletmeye uygun bırak:

AethelionCS
├── DiziboxProvider
├── FutureProvider
├── SharedUtils
└── SharedExtractorHelpers

Ancak ortak modül oluşturmak için erken abstraction yapma.

İkinci provider gerçekten ekleneceği zaman çıkarılabilecek ortak kodları şimdilik yalnızca mantıklı düzeyde organize et.

---

# 39. GITHUB

Repository adı:

`AethelionCS`

GitHub hesabım üzerinden repository oluştur.

Gerekiyorsa:

* main
* develop
* feature branch

kullan.

Ancak gereksiz branch karmaşası oluşturma.

İlk çalışan sürümden önce düzenli commit yap.

Örneğin mantıksal commitler:

* initialize project
* add dizibox provider
* add search and detail parsing
* add episode parsing
* add source resolution
* add subtitle handling
* add tests
* add CI
* documentation.

Git history temiz olsun.

---

# 40. GITHUB ACTIONS

CI kurulmalı.

Push/PR sırasında mümkünse:

* checkout
* Java setup
* Gradle setup
* dependency resolution
* compile
* tests
* extension build

yap.

Upstream CloudStream extensions repository'sinin güncel workflow'larını referans al.

Eski GitHub Action sürümlerini körlemesine kullanma.

---

# 41. PLUGIN REPOSITORY

CloudStream'in güncel extension installation/distribution mekanizmasını araştır.

Gerekliyse:

* repo.json
* plugins.json
* plugin metadata
* GitHub Pages
* generated index

ve upstream'in o an kullandığı mekanizmayı uygula.

Eski tutorial'daki metadata formatını kullanma.

Üretilen extension'ın gerçekten CloudStream tarafından install edilebilir olduğunu doğrula.

---

# 42. README

README oluştur.

İçerik:

# AethelionCS

## Overview

## Supported Providers

### DiziBox

## Features

## Installation

## CloudStream Repository

## Development

## Build

## Testing

## Architecture

## Source Resolution

## Subtitle Support

## Troubleshooting

## Limitations

## Disclaimer

## License

Kurulum adımlarını güncel CloudStream repository installation mekanizmasına göre yaz.

---

# 43. TECHNICAL ARCHITECTURE DOCUMENTATION

README veya docs içine şu akışın teknik açıklamasını ekle:

DiziBox page
↓
Search parser
↓
Detail parser
↓
Season parser
↓
Episode parser
↓
Player/source parser
↓
Source resolver
↓
Existing CloudStream Extractor / Direct Media
↓
ExtractorLink / SubtitleFile
↓
CloudStream Player

Bu belge gelecekte maintenance için önemli.

---

# 44. MAINTAINABILITY

DiziBox HTML yapısı değiştiğinde nerelerin güncellenmesi gerektiğini README'de veya docs'ta açıkla.

Örneğin:

* search selectors
* detail selectors
* season selectors
* episode selectors
* player source extraction
* AJAX endpoint
* source resolver
* subtitle parser.

Maintenance için kritik noktaları yorumlarla belirt.

---

# 45. SECURITY / SECRETS

Repository'ye asla:

* GitHub token
* API key
* password
* browser cookie
* session cookie
* access token
* private key

commit etme.

`.gitignore` oluştur.

CI secret gerekiyorsa GitHub Actions Secrets kullan.

---

# 46. LEGAL / ACCESS BOUNDARY

Provider sadece normal public web erişimi kapsamında içerik çözümlemeli.

Şunları geliştirme:

* DRM kırma
* Widevine/PlayReady key extraction
* CAPTCHA bypass
* Cloudflare challenge bypass
* geo-block bypass
* ISP/DNS restriction bypass
* anti-bot security bypass
* login/paywall bypass
* unauthorized access
* exploit-based access.

Site normal request'lerle erişilebilen public content sağlıyorsa bunu CloudStream'e bağla.

Erişim koruma mekanizması nedeniyle alınamayan içeriği zorla çözmeye çalışma.

---

# 47. CODE QUALITY

Kod:

* Kotlin idiomatic
* null-safe
* coroutine-friendly
* küçük fonksiyonlara ayrılmış
* okunabilir
* test edilebilir
* unnecessary abstraction içermeyen
* deprecated API kullanmayan
* warning'leri mümkün olduğunca temiz

olmalı.

Magic number ve magic string kullanımını azalt.

Selector'ları merkezi/constants halinde tutmak mantıklıysa yap.

---

# 48. PERFORMANCE

Unnecessary network requests yapma.

Search sırasında her sonucu tekrar detail request ile doğrulamak gerekiyorsa bunu gereksiz hale getirecek daha iyi bir parse yöntemi araştır.

Load sırasında yalnızca gerekli endpoint'leri çağır.

Aynı URL'ye duplicate request yapma.

---

# 49. SOURCE HOST CHANGE RESILIENCE

DiziBox zamanla:

VidMoly
→ başka host
→ başka host

gibi source sağlayıcılarını değiştirebilir.

Provider mimarisi yeni host eklemeyi kolaylaştırmalı.

Örneğin:

resolve source
→ detect host
→ loadExtractor

mantığı kullan.

Host isimlerini provider'ın her yerine hardcode etme.

---

# 50. FALLBACK SOURCE DISCOVERY

Source button'ları HTML'de görünüyorsa hepsini parse et.

Bir source hidden attribute'daysa onu da değerlendir.

Player URL'si JavaScript değişkenindeyse onu parse et.

AJAX endpoint ile yükleniyorsa endpoint'i kullan.

Tek iframe bulunca işi bitirme.

---

# 51. REAL-WORLD VALIDATION

Implementation'ın başarılı kabul edilmesi için aşağıdaki zincirin gerçek bir içerikle çalışması gerekir:

SEARCH
→ result visible
→ correct type
→ open detail
→ poster/title/plot
→ seasons
→ episodes
→ select episode
→ sources
→ source resolution
→ playback URL
→ CloudStream player.

Bir noktada kırılıyorsa orijinal response/HTML'i incele ve düzelt.

---

# 52. BUILD VALIDATION

Build sonrası:

* compilation errors
* warnings
* deprecated usage
* test failures
* packaging errors
* plugin metadata errors

kontrol et.

Yerel ortam için güncel doğru Gradle task'ını upstream'den öğren.

Örneğin `./gradlew make` doğru task ise kullan; değilse güncel upstream task'ını kullan.

Windows ortamındaysa uygun:

`gradlew.bat`

kullan.

---

# 53. CI VALIDATION

GitHub'a push ettikten sonra Actions sonuçlarını kontrol et.

Failed action varsa:

1. logu oku
2. problemi teşhis et
3. kod/dependency/config düzelt
4. tekrar push et
5. tekrar doğrula.

CI yeşil olmadan "tamamlandı" deme.

---

# 54. INSTALLATION VALIDATION

Üretilen repository/plugin metadata'nın gerçekten:

* CloudStream'e repository olarak eklenebilir,
* plugin listesinde görünebilir,
* plugin install edilebilir

olduğunu doğrula.

Mümkünse gerçek CloudStream test ortamında validate et.

---

# 55. NO FAKE SUCCESS

Bir özelliği test etmeden çalışıyor deme.

Örneğin:

Subtitle support

sadece kod varsa "çalışıyor" kabul edilmez.

Gerçek subtitle bulunan içerikte test et.

Aynı şekilde:

* multiple source
* direct m3u8
* extractor source
* season
* episode
* type detection

gerçek içeriklerle test edilmeli.

---

# 56. RESEARCH NOTES

Araştırma sırasında:

* hangi upstream API kullanıldı,
* hangi dependency sürümleri seçildi,
* hangi active extension'lar incelendi,
* hangi source hostlar bulundu,
* hangi extractor'lar zaten CloudStream'de vardı,
* DiziBox player akışı nasıl çalışıyor

gibi bilgileri kendi çalışma notlarında tut.

Final raporunda önemli bulguları özetle.

---

# 57. İLK ÇALIŞMA AKIŞI

Şu sırayı uygula:

## PHASE 1 — UPSTREAM RESEARCH

CloudStream upstream'i incele.

## PHASE 2 — ACTIVE EXTENSION RESEARCH

Güncel extension'ları incele.

## PHASE 3 — DIZIBOX REVERSE ENGINEERING

DiziBox'ın:

* search
* detail
* series
* season
* episode
* player
* AJAX
* source
* subtitle

yapısını analiz et.

## PHASE 4 — ARCHITECTURE

Dosya yapısı ve provider mimarisini belirle.

## PHASE 5 — BOOTSTRAP

Repository oluştur.

## PHASE 6 — PROVIDER

Search + detail + season + episode.

## PHASE 7 — SOURCE RESOLUTION

Player/source parser.

## PHASE 8 — EXTRACTOR INTEGRATION

Existing CloudStream extractors.

## PHASE 9 — DIRECT MEDIA

M3U8/MP4/DASH fallback.

## PHASE 10 — SUBTITLE/AUDIO

Mevcut metadata'yı aktar.

## PHASE 11 — TEST

Fixture + gerçek içerik.

## PHASE 12 — CI/CD

GitHub Actions + metadata.

## PHASE 13 — DOCUMENTATION

README + architecture docs.

## PHASE 14 — FINAL VALIDATION

Build + CI + installation.

## PHASE 15 — GITHUB

Final commit + push.

---

# 58. ÖNEMLİ KARAR KURALLARI

Birden fazla çözüm varsa:

ÖNCELİK:

1. güncel resmi CloudStream API
2. mevcut CloudStream extractor
3. upstream pattern
4. basit ve maintainable Kotlin
5. minimum network request
6. graceful failure.

Şunu yapma:

"Eski extension'da böyle yapılmış."

Tek başına bu yeterli gerekçe değildir.

---

# 59. SOURCE RESOLUTION ÖNCELİK SIRASI

Her DiziBox source için:

### A

Existing CloudStream Extractor

→ `loadExtractor(...)`

### B

Direct M3U8/DASH/VIDEO

→ `newExtractorLink(...)`

### C

Player config'ten extract edilebilir normal public stream

→ uygun direct link üret.

### D

Desteklenmeyen yapı

→ graceful failure + diagnostic log.

Bir source'un desteklenmemesi diğer source'ların resolution'ını engellememeli.

---

# 60. SONUÇ RAPORU

İş tamamen bittiğinde bana şunları bildir:

1. GitHub repository URL
2. branch
3. latest commit SHA
4. kullanılan CloudStream API/upstream commit/version
5. Kotlin version
6. Gradle version
7. AGP version
8. compileSdk / targetSdk
9. dependency listesi
10. DiziBox parser mimarisi
11. search desteği
12. movie desteği
13. TV series desteği
14. season desteği
15. episode desteği
16. source resolution desteği
17. kullanılan mevcut CloudStream extractor'lar
18. direct media desteği
19. quality detection
20. subtitle desteği
21. audio/dubbing desteği
22. fixture tests
23. gerçek içerik testleri
24. build sonucu
25. GitHub Actions sonucu
26. installation validation sonucu
27. bilinen sorunlar
28. DiziBox HTML değişirse muhtemelen güncellenecek alanlar.

---

# 61. BİTİRME KRİTERİ

Aşağıdakiler tamamlanmadan proje "finished" kabul edilmez:

[ ] Repository oluşturuldu
[ ] Güncel CloudStream architecture araştırıldı
[ ] Güncel API'ler doğrulandı
[ ] Güncel dependency'ler doğrulandı
[ ] DiziBox search çalışıyor
[ ] Film type doğru
[ ] TV series type doğru
[ ] Film detail çalışıyor
[ ] Series detail çalışıyor
[ ] Season parsing çalışıyor
[ ] Episode parsing çalışıyor
[ ] Player/source parsing çalışıyor
[ ] Multiple source desteği çalışıyor
[ ] Existing CloudStream extractor entegrasyonu çalışıyor
[ ] Direct M3U8/MP4/DASH gerektiğinde çalışıyor
[ ] Quality metadata mümkün olduğunca aktarılıyor
[ ] Subtitle callback çalışıyor
[ ] Source failure isolation var
[ ] Duplicate source engelleniyor
[ ] Network failure handling var
[ ] Parser fallback mekanizması var
[ ] Fixture tests var
[ ] Gerçek içeriklerle test yapıldı
[ ] Gradle build başarılı
[ ] GitHub Actions başarılı
[ ] Plugin metadata doğru
[ ] Installation mekanizması doğrulandı
[ ] README tamamlandı
[ ] GitHub'a push edildi.

---

# 62. ÇALIŞMA TARZI

Benden sürekli "şimdi bunu yapayım mı?" şeklinde onay isteme.

Küçük kararları kendin ver.

Bir problem çıkarsa:

1. logu oku,
2. upstream'i araştır,
3. mevcut implementation'ları araştır,
4. en doğru çözümü uygula,
5. build/test yap,
6. gerekirse refactor et,
7. devam et.

Ancak güvenlik/erişim kontrolü bypass'ı gerektiren bir noktaya gelirse o kısmı uygulama; problemi açıkça raporla.

---

# 63. İLK AKSİYON

ŞİMDİ BAŞLA.

İlk olarak:

1. güncel CloudStream upstream'i araştır,
2. güncel extension template'ini araştır,
3. güncel dependency/version setini çıkar,
4. mevcut ExtractorApi ve `loadExtractor` / `newExtractorLink` API'lerini gerçek source kodundan doğrula,
5. güncel DiziBox yapısını analiz et,
6. DiziBox source hostlarını keşfet,
7. bunların CloudStream'de mevcut extractor'larını kontrol et,
8. mimariyi belirle,
9. repository'yi oluştur,
10. implementation'a geç.

Araştırma sonunda sadece teorik bir plan bırakma.

Araştırmadan sonra doğrudan implementasyona devam et.

HEDEF:
AethelionCS'ın gerçek, derlenebilir, test edilmiş ve CloudStream'e kurulabilir ilk DiziBox provider sürümünü GitHub'a koymak.
