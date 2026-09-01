package com.aethelioncs.dizibox

import org.jsoup.Jsoup
import java.net.URI

object DiziboxParser {

    private val SEASON_TEXT_REGEX = Regex("""(\d+)\s*\.\s*[sS]ezon""")
    private val SEASON_URL_REGEX = Regex("""-(\d+)-sezon-""")
    
    private val EPISODE_TEXT_REGEX = Regex("""(\d+)\s*\.\s*[bB][öoÖO]l[üuÜU]m""")
    private val EPISODE_URL_REGEX = Regex("""-(\d+)-bolum""")

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

    fun parseSearchResults(html: String, mainUrl: String): List<Pair<String, String>> {
        val doc = Jsoup.parse(html)
        val results = mutableListOf<Pair<String, String>>()
        val seenUrls = mutableSetOf<String>()

        val links = doc.select("a[href*=\"/diziler/\"]")
        for (a in links) {
            val title = a.text().trim()
            val href = fixUrl(a.attr("href"), mainUrl) ?: continue
            if (title.isNotEmpty() && !href.endsWith("/diziler/") && seenUrls.add(href)) {
                results.add(Pair(title, href))
            }
        }
        return results
    }

    fun parseSeasonTabs(html: String, mainUrl: String): List<SeasonTab> {
        val doc = Jsoup.parse(html)
        val seasonTabs = mutableListOf<SeasonTab>()
        val seenSeasons = mutableSetOf<Int>()

        val links = doc.select("a[href*=\"/dizi/\"][href*=\"-sezon-\"]")
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

        val links = doc.select(".season-episode a, a[href*=\"-sezon-\"][href*=\"-bolum-\"]")
        for (a in links) {
            val href = fixUrl(a.attr("href"), mainUrl) ?: continue
            if (!seenUrls.add(href)) continue

            val text = a.text().trim()
            val parent = a.parent()
            val parentText = parent?.text() ?: text

            val season = parseSeasonNumber(parentText, href) ?: defaultSeason
            val episode = parseEpisodeNumber(parentText, href) ?: 1

            // Name / Title extraction
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
