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

        val rawTitle = doc.selectFirst("h1.entry-title, h1, .title, .post-title")?.text()?.trim() ?: "DiziBox"
        val title = rawTitle.replace(Regex("""\s*izle.*"""), "").trim()

        val rawPoster = doc.selectFirst(".poster img, .entry-content img, article img")?.attr("data-src")?.ifEmpty { null }
            ?: doc.selectFirst(".poster img, .entry-content img, article img")?.attr("src")?.ifEmpty { null }
        val poster = DiziboxParser.cleanPosterUrl(rawPoster, mainUrl)

        val plot = doc.selectFirst(".description, .entry-content p, .overview")?.text()?.trim()
        val year = doc.selectFirst(".release-year, .year")?.text()?.toIntOrNull()

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
