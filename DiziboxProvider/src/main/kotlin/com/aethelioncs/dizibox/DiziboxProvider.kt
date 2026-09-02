package com.aethelioncs.dizibox

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.Jsoup
import java.net.URLEncoder

class DiziboxProvider : MainAPI() {
    override var name = "DiziBox"
    override var mainUrl = "https://www.dizibox.live"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = false

    override val supportedTypes = setOf(
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "https://www.dizibox.live/diziler/?orderby=popular" to "Popüler Diziler",
        "https://www.dizibox.live/" to "Son Eklenen Bölümler",
        "https://www.dizibox.live/diziler/" to "Tüm Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (request.data.startsWith("http")) request.data else "$mainUrl${request.data}"
        val pageUrl = if (page > 1) {
            if (url.contains("?")) {
                url.replace("?", "page/$page/?")
            } else {
                "${url.trimEnd('/')}/page/$page/"
            }
        } else {
            url
        }

        val html = app.get(pageUrl, referer = mainUrl).text
        val results = if (request.name == "Son Eklenen Bölümler") {
            val sectionItems = DiziboxParser.parseHomePageSection(html, "Bölümler", mainUrl)
            if (sectionItems.isNotEmpty()) sectionItems else DiziboxParser.parseSearchResults(html, mainUrl)
        } else {
            DiziboxParser.parseSearchResults(html, mainUrl)
        }

        val homeItems = results.map { item ->
            newTvSeriesSearchResponse(item.title, item.url, TvType.TvSeries) {
                this.posterUrl = item.posterUrl
            }
        }

        return newHomePageResponse(
            listOf(HomePageList(request.name, homeItems)),
            hasNext = homeItems.isNotEmpty() && request.name == "Tüm Diziler"
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val searchUrl = "$mainUrl/?s=$encodedQuery"
        val html = app.get(searchUrl, referer = mainUrl).text
        val results = DiziboxParser.parseSearchResults(html, mainUrl)

        return results.map { item ->
            newTvSeriesSearchResponse(item.title, item.url, TvType.TvSeries) {
                this.posterUrl = item.posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val absoluteUrl = DiziboxParser.fixUrl(url, mainUrl) ?: url
        val html = app.get(absoluteUrl, referer = mainUrl).text
        val doc = Jsoup.parse(html)

        // 1. Robust Title Extraction (Avoiding hidden auth / login modals)
        val ogTitle = doc.selectFirst("meta[property=\"og:title\"]")?.attr("content")?.trim()
        val overviewTitle = doc.selectFirst(".tv-overview .title-terms, .tv-overview h1, .tv-overview .tv-title")?.text()?.trim()
        val h1Title = doc.selectFirst("main h1, .content h1, h1.entry-title")?.text()?.trim()

        val rawTitle = when {
            !ogTitle.isNullOrBlank() && !ogTitle.equals("ÜYE GİRİŞİ", ignoreCase = true) -> ogTitle
            !overviewTitle.isNullOrBlank() && !overviewTitle.equals("ÜYE GİRİŞİ", ignoreCase = true) -> overviewTitle
            !h1Title.isNullOrBlank() && !h1Title.equals("ÜYE GİRİŞİ", ignoreCase = true) -> h1Title
            else -> "DiziBox"
        }
        val title = rawTitle
            .replace(Regex("""\s*\d+\.\d+/10.*"""), "")
            .replace(Regex("""\s*izle.*""", RegexOption.IGNORE_CASE), "")
            .trim()

        // 2. Poster Extraction
        val overviewPoster = doc.selectFirst(".tv-overview img, img.main-cover")?.attr("src")?.ifEmpty { null }
            ?: doc.selectFirst(".tv-overview img, img.main-cover")?.attr("data-src")?.ifEmpty { null }
        val ogPoster = doc.selectFirst("meta[property=\"og:image\"]")?.attr("content")?.ifEmpty { null }
        val poster = DiziboxParser.cleanPosterUrl(overviewPoster ?: ogPoster, mainUrl)

        // 3. Plot / Description Extraction
        val overviewPlot = doc.selectFirst(".tv-overview p, .overview, .description")?.text()?.trim()
        val ogPlot = doc.selectFirst("meta[property=\"og:description\"]")?.attr("content")?.trim()
        val metaPlot = doc.selectFirst("meta[name=\"description\"]")?.attr("content")?.trim()
        val plot = when {
            !overviewPlot.isNullOrBlank() && overviewPlot.length > 20 -> overviewPlot
            !ogPlot.isNullOrBlank() && ogPlot.length > 20 && !ogPlot.contains("elit site") -> ogPlot
            !metaPlot.isNullOrBlank() && metaPlot.length > 20 -> metaPlot
            else -> overviewPlot ?: ogPlot ?: metaPlot
        }

        // 4. Year & Tags Extraction
        val overviewText = doc.selectFirst(".tv-overview")?.text() ?: ""
        val year = Regex("""\b(19\d\d|20\d\d)\b""").find(overviewText)?.groupValues?.get(1)?.toIntOrNull()
        val tags = doc.select(".tv-overview a[href*=\"/tur/\"], .tv-overview a[href*=\"/kategori/\"]").map { it.text().trim() }

        // 5. Season and Episode Extraction
        val seasonTabs = DiziboxParser.parseSeasonTabs(html, mainUrl)
        val allEpisodes = mutableListOf<Episode>()

        if (seasonTabs.isNotEmpty()) {
            for (tab in seasonTabs) {
                val tabEpisodes = if (tab.url == absoluteUrl) {
                    DiziboxParser.parseEpisodes(html, tab.seasonNumber, mainUrl)
                } else {
                    try {
                        val tabHtml = app.get(tab.url, referer = absoluteUrl).text
                        DiziboxParser.parseEpisodes(tabHtml, tab.seasonNumber, mainUrl)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                for (ep in tabEpisodes) {
                    allEpisodes.add(
                        newEpisode(ep.url) {
                            this.name = ep.name
                            this.season = ep.season
                            this.episode = ep.episode
                        }
                    )
                }
            }
        } else {
            val singleSeasonEpisodes = DiziboxParser.parseEpisodes(html, 1, mainUrl)
            for (ep in singleSeasonEpisodes) {
                allEpisodes.add(
                    newEpisode(ep.url) {
                        this.name = ep.name
                        this.season = ep.season
                        this.episode = ep.episode
                    }
                )
            }
        }

        return newTvSeriesLoadResponse(title, absoluteUrl, TvType.TvSeries, allEpisodes) {
            this.posterUrl = poster
            this.plot = plot
            this.year = year
            this.tags = if (tags.isNotEmpty()) tags else null
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val episodeUrl = DiziboxParser.fixUrl(data, mainUrl) ?: data
        return DiziboxSourceResolver.resolveEpisodeLinks(
            episodeUrl = episodeUrl,
            mainUrl = mainUrl,
            subtitleCallback = subtitleCallback,
            callback = callback
        )
    }
}
