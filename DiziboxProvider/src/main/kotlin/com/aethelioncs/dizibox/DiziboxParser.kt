package com.aethelioncs.dizibox

import org.jsoup.Jsoup
import java.net.URI

object DiziboxParser {

    private val SEASON_TEXT_REGEX = Regex("""(\d+)\s*\.\s*[sS]ezon""")
    private val SEASON_URL_REGEX = Regex("""-(\d+)-sezon-""")
    
    private val EPISODE_TEXT_REGEX = Regex("""(\d+)\s*\.\s*[bB][öoÖO]l[üuÜU]m""")
    private val EPISODE_URL_REGEX = Regex("""-(\d+)-bolum""")

    private val EXCLUDED_PATHS = setOf(
        "/arsiv", "/arsiv/",
        "/dizi-takvimi", "/dizi-takvimi/",
        "/yardim", "/yardim/",
        "/iletisim", "/iletisim/",
        "/tum-bolumler", "/tum-bolumler/",
        "/diziler", "/diziler/",
        "/wp-login.php", "/login"
    )

    fun fixUrl(url: String?, mainUrl: String): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return try {
            if (trimmed.startsWith("//")) {
                "https:$trimmed"
            } else if (trimmed.startsWith("/")) {
                val uri = URI(mainUrl)
                "${uri.scheme}://${uri.host}$trimmed"
            } else if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    fun isSeriesDetailUrl(url: String): Boolean {
        val fixed = fixUrl(url, "https://www.dizibox.live") ?: return false
        val uri = try { URI(fixed) } catch (e: Exception) { return false }
        val path = uri.path?.trimEnd('/') ?: ""

        if (EXCLUDED_PATHS.any { it.trimEnd('/') == path }) return false
        if (path.contains("takvim") || path.contains("yardim") || path.contains("iletisim") || path.contains("login") || path.contains("auth")) {
            return false
        }
        if (path.contains("-bolum-") || path.contains("-bolum")) {
            return false
        }

        return (path.startsWith("/diziler/") && path != "/diziler") ||
                path.startsWith("/dizi/") ||
                (path.endsWith("-izle") && !path.contains("-sezon-"))
    }

    fun isContentUrl(url: String): Boolean {
        val fixed = fixUrl(url, "https://www.dizibox.live") ?: return false
        val uri = try { URI(fixed) } catch (e: Exception) { return false }
        val path = uri.path?.trimEnd('/') ?: ""

        if (EXCLUDED_PATHS.any { it.trimEnd('/') == path }) return false
        if (path.contains("takvim") || path.contains("yardim") || path.contains("iletisim") || path.contains("login") || path.contains("auth")) {
            return false
        }

        return isSeriesDetailUrl(url) || path.contains("-bolum-") || path.contains("-sezon-")
    }

    fun cleanPosterUrl(rawUrl: String?, mainUrl: String): String? {
        if (rawUrl.isNullOrBlank() || rawUrl.startsWith("data:")) return null
        val fixed = fixUrl(rawUrl, mainUrl) ?: return null
        if (fixed.contains("altyazi.png") || fixed.contains("default") || fixed.contains("blank") || fixed.contains("data:image")) {
            return null
        }
        return fixed.replace(Regex("""-\d+x\d+\."""), "-200x290.")
    }

    fun parseSeasonNumber(text: String, href: String = ""): Int? {
        SEASON_TEXT_REGEX.find(text)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        SEASON_URL_REGEX.find(href)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        return null
    }

    fun parseEpisodeNumber(text: String, href: String = ""): Int? {
        EPISODE_TEXT_REGEX.find(text)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        EPISODE_URL_REGEX.find(href)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        return null
    }

    fun parseSearchResults(html: String, mainUrl: String): List<DiziboxSearchItem> {
        val doc = Jsoup.parse(html)
        val results = mutableListOf<DiziboxSearchItem>()
        val seenUrls = mutableSetOf<String>()

        val cards = doc.select(".content-wrapper article, .content-wrapper .post, .content-wrapper .item, .content-wrapper .tv-item, .tv-list article, .tv-list .item")
        for (card in cards) {
            if (card.parents().any { it.tagName() == "nav" || it.tagName() == "header" || it.tagName() == "footer" || it.hasClass("ajax-auth") || it.hasClass("menu") || it.hasClass("navigation") }) {
                continue
            }

            val a = card.selectFirst("a[href*=\"/diziler/\"], a[href*=\"-izle\"], a[href*=\"/dizi/\"]") ?: card.selectFirst("a") ?: continue
            val href = fixUrl(a.attr("href"), mainUrl) ?: continue
            if (!isContentUrl(href) || !seenUrls.add(href)) continue

            val img = card.selectFirst("img")
            val rawImg = img?.attr("data-src")?.ifEmpty { null }
                ?: img?.attr("data-lazy-src")?.ifEmpty { null }
                ?: img?.attr("srcset")?.split(" ")?.firstOrNull()
                ?: img?.attr("src")?.takeIf { !it.startsWith("data:") }
            val poster = cleanPosterUrl(rawImg, mainUrl)

            val titleEl = card.selectFirst(".tv-title, .title, .post-title, h2, h3, h4") ?: a
            var title = titleEl.text().trim()
            if (title.equals("ÜYE GİRİŞİ", ignoreCase = true) || title.equals("Arşiv", ignoreCase = true) || title.contains("Takvim", ignoreCase = true)) {
                continue
            }
            title = title
                .replace(Regex("""\s*\d+\.\d+/10.*"""), "")
                .replace(Regex("""\s*izle.*""", RegexOption.IGNORE_CASE), "")
                .trim()

            if (title.isNotEmpty()) {
                results.add(DiziboxSearchItem(title, href, poster))
            }
        }

        return results
    }

    fun parseHomePageSection(html: String, sectionTitlePattern: String, mainUrl: String): List<DiziboxSearchItem> {
        val doc = Jsoup.parse(html)
        val results = mutableListOf<DiziboxSearchItem>()
        val seenUrls = mutableSetOf<String>()

        val blocks = doc.select(".content-wrapper > div, .row, .full-width, .widget, .block, section")
        for (block in blocks) {
            val blockHeader = block.selectFirst(".title, h2, h3, h4, .widget-title")?.text() ?: ""
            if (blockHeader.contains(sectionTitlePattern, ignoreCase = true)) {
                val cards = block.select("article, .post, .item, .tv-item, li")
                for (card in cards) {
                    if (card.parents().any { it.tagName() == "nav" || it.hasClass("ajax-auth") || it.hasClass("menu") }) continue

                    val a = card.selectFirst("a") ?: continue
                    val href = fixUrl(a.attr("href"), mainUrl) ?: continue
                    if (!isContentUrl(href) || !seenUrls.add(href)) continue

                    val img = card.selectFirst("img")
                    val rawImg = img?.attr("data-src")?.ifEmpty { null }
                        ?: img?.attr("data-lazy-src")?.ifEmpty { null }
                        ?: img?.attr("srcset")?.split(" ")?.firstOrNull()
                        ?: img?.attr("src")?.takeIf { !it.startsWith("data:") }
                    val poster = cleanPosterUrl(rawImg, mainUrl)

                    val titleEl = card.selectFirst(".tv-title, .title, .post-title, h2, h3, h4") ?: a
                    var title = titleEl.text().trim()
                    if (title.equals("ÜYE GİRİŞİ", ignoreCase = true)) continue
                    title = title
                        .replace(Regex("""\s*\d+\.\d+/10.*"""), "")
                        .replace(Regex("""\s*izle.*""", RegexOption.IGNORE_CASE), "")
                        .trim()

                    if (title.isNotEmpty()) {
                        results.add(DiziboxSearchItem(title, href, poster))
                    }
                }
            }
        }
        return results
    }

    fun parseSeasonTabs(html: String, mainUrl: String): List<SeasonTab> {
        val doc = Jsoup.parse(html)
        val seasonTabs = mutableListOf<SeasonTab>()
        val seenSeasons = mutableSetOf<Int>()

        val links = doc.select("a[href*=\"/dizi/\"], a[href*=\"-sezon-\"]")
        for (a in links) {
            val href = fixUrl(a.attr("href"), mainUrl) ?: continue
            val text = a.text().trim()
            val sNum = parseSeasonNumber(text, href)
            if (sNum != null && seenSeasons.add(sNum)) {
                seasonTabs.add(SeasonTab(sNum, href))
            }
        }
        return seasonTabs.sortedBy { it.seasonNumber }
    }

    fun parseEpisodes(html: String, defaultSeason: Int, mainUrl: String): List<ParsedEpisode> {
        val doc = Jsoup.parse(html)
        val episodes = mutableListOf<ParsedEpisode>()
        val seenUrls = mutableSetOf<String>()

        val episodeContainers = doc.select(".season-episode, .episodes-list, .tv-episodes, .box-body, article.post")
        val links = if (episodeContainers.isNotEmpty()) {
            episodeContainers.select("a[href*=\"-sezon-\"][href*=\"-bolum-\"], a[href*=\"-bolum-\"]")
        } else {
            doc.select(".content-wrapper a[href*=\"-sezon-\"][href*=\"-bolum-\"], .content-wrapper a[href*=\"-bolum-\"]")
        }

        for (a in links) {
            val href = fixUrl(a.attr("href"), mainUrl) ?: continue
            if (!href.contains("-bolum") || !seenUrls.add(href)) continue

            val text = a.text().trim()
            val parent = a.parent()
            val parentText = parent?.text() ?: text

            val season = parseSeasonNumber(parentText, href) ?: defaultSeason
            val episode = parseEpisodeNumber(parentText, href) ?: 1

            val epName = if (text.contains("(") && text.contains(")")) {
                text.substringAfter("(").substringBefore(")")
            } else if (text.isNotBlank()) {
                text
            } else {
                "$season. Sezon $episode. Bölüm"
            }

            episodes.add(
                ParsedEpisode(
                    name = epName,
                    season = season,
                    episode = episode,
                    url = href
                )
            )
        }
        return episodes.sortedWith(compareBy({ it.season }, { it.episode }))
    }

    fun extractIframes(html: String, baseUrl: String): List<String> {
        val doc = Jsoup.parse(html)
        val iframes = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        for (ifr in doc.select("iframe")) {
            val src = ifr.attr("src").ifBlank { ifr.attr("data-src") }
            val absolute = fixUrl(src, baseUrl)
            if (!absolute.isNullOrBlank() && seen.add(absolute)) {
                iframes.add(absolute)
            }
        }
        return iframes
    }
}
