package com.aethelioncs.dizibox

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI

object DiziboxSourceResolver {

    private const val MAX_IFRAME_DEPTH = 3
    private const val MAX_SOURCE_CANDIDATES = 20

    suspend fun resolveEpisodeLinks(
        episodeUrl: String,
        mainUrl: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var foundAnyLink = false
        val visitedUrls = mutableSetOf<String>()
        val seenSubtitleKeys = mutableSetOf<String>()

        val safeSubtitleCallback: (SubtitleFile) -> Unit = { sub ->
            val key = "${sub.lang}_${sub.url}"
            if (seenSubtitleKeys.add(key)) {
                subtitleCallback(sub)
            }
        }

        val safeLinkCallback: (ExtractorLink) -> Unit = { link ->
            foundAnyLink = true
            callback(link)
        }

        suspend fun resolveUrlRecursive(url: String, depth: Int, referer: String?): Boolean {
            if (depth > MAX_IFRAME_DEPTH || visitedUrls.size >= MAX_SOURCE_CANDIDATES) return false
            if (!visitedUrls.add(url)) return false

            val uri = try { URI(url) } catch (e: Exception) { return false }
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme != "http" && scheme != "https") return false

            val host = uri.host?.lowercase() ?: ""

            // 1. Direct Media Check (m3u8, mp4)
            if (url.contains(".m3u8") || url.contains(".mp4")) {
                val isM3u8 = url.contains(".m3u8")
                safeLinkCallback(
                    newExtractorLink(
                        source = "DiziBox",
                        name = "DiziBox Direct Stream",
                        url = url,
                        type = if (isM3u8) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        this.referer = referer ?: mainUrl
                        this.quality = Qualities.Unknown.value
                    }
                )
                return true
            }

            // 2. Molystream / Vidmoly Multi-Variant Stream Extraction
            if (host.contains("molystream.org") && url.contains("/embed/")) {
                val baseId = if (url.contains("/embed/sheila/")) {
                    url.substringAfter("/embed/sheila/").substringBefore("/")
                } else {
                    url.substringAfter("/embed/").substringBefore("/")
                }

                if (baseId.isNotEmpty()) {
                    // A. Master Playlist (Adaptive Bitrate - Auto)
                    val masterUrl = "https://dbx.molystream.org/embed/sheila/$baseId"
                    safeLinkCallback(
                        newExtractorLink(
                            source = "Molystream",
                            name = "Molystream (Adaptive - Otomatik)",
                            url = masterUrl,
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.referer = url
                            this.quality = Qualities.Unknown.value
                        }
                    )

                    // B. 1080p Stream Variant
                    safeLinkCallback(
                        newExtractorLink(
                            source = "Molystream",
                            name = "Molystream 1080p (Full HD)",
                            url = "https://dbx.molystream.org/embed/$baseId/q/1",
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.referer = url
                            this.quality = Qualities.P1080.value
                        }
                    )

                    // C. 720p Stream Variant
                    safeLinkCallback(
                        newExtractorLink(
                            source = "Molystream",
                            name = "Molystream 720p (HD)",
                            url = "https://dbx.molystream.org/embed/$baseId/q/2",
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.referer = url
                            this.quality = Qualities.P720.value
                        }
                    )

                    // D. 480p Stream Variant (Düşük Hız / Akıcı)
                    safeLinkCallback(
                        newExtractorLink(
                            source = "Molystream",
                            name = "Molystream 480p (Akıcı)",
                            url = "https://dbx.molystream.org/embed/$baseId/q/3",
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.referer = url
                            this.quality = Qualities.P480.value
                        }
                    )
                }
            }

            // 3. Known Extractor Match via loadExtractor (Vidmoly, etc.)
            try {
                val extractorLoaded = loadExtractor(
                    url = url,
                    referer = referer ?: mainUrl,
                    subtitleCallback = safeSubtitleCallback,
                    callback = safeLinkCallback
                )
                if (extractorLoaded && foundAnyLink) {
                    return true
                }
            } catch (e: Exception) {
                // Isolated exception, continue
            }

            // 4. Nested Iframe Crawling
            try {
                val html = app.get(url, referer = referer ?: mainUrl).text
                val iframes = DiziboxParser.extractIframes(html, url)
                for (nestedIframe in iframes) {
                    if (visitedUrls.size >= MAX_SOURCE_CANDIDATES) break
                    resolveUrlRecursive(nestedIframe, depth + 1, url)
                }
            } catch (e: Exception) {
                // Fail gracefully
            }

            return foundAnyLink
        }

        resolveUrlRecursive(episodeUrl, depth = 1, referer = mainUrl)
        return foundAnyLink
    }
}
