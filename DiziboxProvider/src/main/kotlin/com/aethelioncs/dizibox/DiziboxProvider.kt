package com.aethelioncs.dizibox

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.Jsoup
import java.net.URLEncoder

@CloudstreamPlugin
class DiziboxProvider : MainAPI() {
    override var name = "DiziBox"
    override var mainUrl = "https://www.dizibox.live"
    override var lang = "tr"
    override var supportedTypes = setOf(TvType.TvSeries)
    override var hasMainPage = true

    override val mainPage = mainPageOf(
        "https://www.dizibox.live/diziler/" to "Tüm Diziler",
        "https://www.dizibox.live/arsiv/" to "Arşiv"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (request.data.startsWith("http")) request.data else "$mainUrl${request.data}"
        val html = app.get(url, referer = mainUrl).text
        val results = DiziboxParser.parseSearchResults(html, mainUrl)

        val homeItems = results.map { (title, link) ->
            newTvSeriesSearchResponse(title, link, TvType.TvSeries)
        }

        return newHomePageResponse(
            listOf(HomePageList(request.name, homeItems)),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val archiveSearchUrl = "$mainUrl/arsiv/?&dizi=$encodedQuery"

        return try {
            val html = app.get(archiveSearchUrl, referer = mainUrl).text
            val results = DiziboxParser.parseSearchResults(html, mainUrl)
            
            val filtered = results.filter { (title, _) ->
                title.contains(query, ignoreCase = true)
            }

            val finalResults = if (filtered.isNotEmpty()) filtered else results

            finalResults.map { (title, link) ->
                newTvSeriesSearchResponse(title, link, TvType.TvSeries)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val html = app.get(url, referer = mainUrl).text
        val doc = Jsoup.parse(html)

        val title = doc.select("h1").firstOrNull()?.text()?.trim() ?: "DiziBox Series"
        val poster = doc.select("img.main-cover").firstOrNull()?.attr("src")
            ?: doc.select(".poster img, .series-poster img").firstOrNull()?.attr("src")
        val absolutePoster = DiziboxParser.fixUrl(poster, mainUrl)

        val plot = doc.select(".summary, .entry-content, .grid-box p, p.story").firstOrNull()?.text()?.trim()

        val allEpisodes = mutableListOf<Episode>()

        // Check multi-season tabs
        val seasonTabs = DiziboxParser.parseSeasonTabs(html, mainUrl)

        if (seasonTabs.isNotEmpty()) {
            for (tab in seasonTabs) {
                try {
                    val seasonHtml = app.get(tab.url, referer = url).text
                    val seasonEpisodes = DiziboxParser.parseEpisodes(seasonHtml, tab.seasonNumber, mainUrl)
                    for (ep in seasonEpisodes) {
                        allEpisodes.add(
                            newEpisode(ep.url) {
                                this.name = ep.name
                                this.season = ep.season
                                this.episode = ep.episode
                                this.posterUrl = absolutePoster
                            }
                        )
                    }
                } catch (e: Exception) {
                    // Fail gracefully for this single season
                }
            }
        } else {
            // Single-season layout
            val singleSeasonEpisodes = DiziboxParser.parseEpisodes(html, defaultSeason = 1, mainUrl = mainUrl)
            for (ep in singleSeasonEpisodes) {
                allEpisodes.add(
                    newEpisode(ep.url) {
                        this.name = ep.name
                        this.season = ep.season
                        this.episode = ep.episode
                        this.posterUrl = absolutePoster
                    }
                )
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, allEpisodes) {
            this.posterUrl = absolutePoster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isDataJob: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return DiziboxSourceResolver.resolveEpisodeLinks(
            episodeUrl = data,
            mainUrl = mainUrl,
            subtitleCallback = subtitleCallback,
            callback = callback
        )
    }
}
