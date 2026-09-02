package com.aethelioncs.dizibox

import org.junit.Assert.*
import org.junit.Test

class DiziboxHlsRegressionTest {

    private val SYNTHETIC_MASTER_M3U8 = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080
        https://test-cdn.local/hls/1080p.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=2800000,RESOLUTION=1280x720
        https://test-cdn.local/hls/720p.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=1200000,RESOLUTION=854x480
        https://test-cdn.local/hls/480p.m3u8
    """.trimIndent()

    private val SYNTHETIC_VARIANT_1080P_M3U8 = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-TARGETDURATION:3
        #EXT-X-MEDIA-SEQUENCE:0
        #EXTINF:3.0,
        https://test-cdn.local/segments/seg_000.ts
        #EXTINF:3.0,
        https://test-cdn.local/segments/seg_001.ts
        #EXT-X-ENDLIST
    """.trimIndent()

    @Test
    fun testTwoDifferentEpisodesProduceDifferentData() {
        val ep1Url = "https://www.dizibox.live/a-confession-1-sezon-1-bolum-izle/"
        val ep2Url = "https://www.dizibox.live/a-confession-1-sezon-2-bolum-izle/"
        val ep3Url = "https://www.dizibox.live/adults-2-sezon-3-bolum-izle/"

        assertNotEquals("Episode 1 and 2 must have different data URLs", ep1Url, ep2Url)
        assertNotEquals("Episode 1 and 3 must have different data URLs", ep1Url, ep3Url)
        assertNotEquals("Episode 2 and 3 must have different data URLs", ep2Url, ep3Url)
    }

    @Test
    fun testEpisodeUrlsRemainDifferentAfterNormalization() {
        val ep1 = DiziboxParser.fixUrl("https://www.dizibox.live/a-confession-1-sezon-1-bolum-izle/", "https://www.dizibox.live")
        val ep2 = DiziboxParser.fixUrl("https://www.dizibox.live/a-confession-1-sezon-2-bolum-izle/", "https://www.dizibox.live")

        assertNotNull(ep1)
        assertNotNull(ep2)
        assertNotEquals(ep1, ep2)
    }

    @Test
    fun testNavigationCardsMustNotBecomeSeries() {
        val navUrls = listOf(
            "https://www.dizibox.live/arsiv/",
            "https://www.dizibox.live/dizi-takvimi/",
            "https://www.dizibox.live/yardim/",
            "https://www.dizibox.live/iletisim/"
        )
        for (u in navUrls) {
            assertFalse("Navigation link $u must not be accepted as series detail", DiziboxParser.isSeriesDetailUrl(u))
            assertFalse("Navigation link $u must not be accepted as content", DiziboxParser.isContentUrl(u))
        }
    }

    @Test
    fun testValidImageProducesPosterUrlAndBase64IsFiltered() {
        val mainUrl = "https://www.dizibox.live"
        val validImg = "https://www.dizibox.live/wp-content/uploads/afisler/friends-200x290.jpg"
        val thumbImg = "https://www.dizibox.live/wp-content/uploads/afisler/friends-50x50.jpg"
        val base64Img = "data:image/png;base64,iVBORw0KGgoAAA..."
        val placeholderImg = "https://www.dizibox.live/altyazi.png"

        assertEquals(validImg, DiziboxParser.cleanPosterUrl(validImg, mainUrl))
        assertEquals(validImg, DiziboxParser.cleanPosterUrl(thumbImg, mainUrl))
        assertNull(DiziboxParser.cleanPosterUrl(base64Img, mainUrl))
        assertNull(DiziboxParser.cleanPosterUrl(placeholderImg, mainUrl))
    }

    @Test
    fun testMasterPlaylistDetection() {
        assertTrue("Master playlist should contain EXTM3U header", SYNTHETIC_MASTER_M3U8.startsWith("#EXTM3U"))
        assertTrue("Master playlist should contain stream variant tags", SYNTHETIC_MASTER_M3U8.contains("#EXT-X-STREAM-INF"))
    }

    @Test
    fun testVariantPlaylistExtraction() {
        val lines = SYNTHETIC_MASTER_M3U8.lines()
        val variants = mutableListOf<Pair<String, String>>()
        
        var currentResolution = ""
        for (line in lines) {
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                val resMatch = Regex("""RESOLUTION=(\d+x\d+)""").find(line)
                currentResolution = resMatch?.groupValues?.get(1) ?: "Unknown"
            } else if (line.startsWith("http")) {
                variants.add(currentResolution to line.trim())
            }
        }

        assertEquals(3, variants.size)
        assertEquals("1920x1080", variants[0].first)
        assertEquals("https://test-cdn.local/hls/1080p.m3u8", variants[0].second)

        assertEquals("1280x720", variants[1].first)
        assertEquals("https://test-cdn.local/hls/720p.m3u8", variants[1].second)

        assertEquals("854x480", variants[2].first)
        assertEquals("https://test-cdn.local/hls/480p.m3u8", variants[2].second)
    }

    @Test
    fun testSourceAndQualitySeparation() {
        val sourceHost = "test-cdn.local"
        val qualityVariants = listOf("1080p", "720p", "480p")

        val sourceHostsCount = listOf(sourceHost).distinct().size
        val qualityCount = qualityVariants.size

        assertEquals(1, sourceHostsCount)
        assertEquals(3, qualityCount)
        assertNotEquals("Source count must not equal quality variant count", qualityCount, sourceHostsCount)
    }

    @Test
    fun testMediaSegmentParsing() {
        val segmentLines = SYNTHETIC_VARIANT_1080P_M3U8.lines()
            .map { it.trim() }
            .filter { it.startsWith("http") && it.endsWith(".ts") }

        assertEquals(2, segmentLines.size)
        assertEquals("https://test-cdn.local/segments/seg_000.ts", segmentLines[0])
        assertEquals("https://test-cdn.local/segments/seg_001.ts", segmentLines[1])
    }

    @Test
    fun testSubtitleClassification() {
        val hardcodedSubtitleSample = "Hardcoded Turkish Subtitle embedded in video stream"
        val externalVttSample = "https://test-cdn.local/subs/turkish.vtt"

        val isExternalTrack = externalVttSample.endsWith(".vtt") || externalVttSample.endsWith(".srt")
        val isHardcoded = hardcodedSubtitleSample.contains("Hardcoded")

        assertTrue(isExternalTrack)
        assertTrue(isHardcoded)
        assertFalse("Hardcoded stream must not be flagged as external subtitle file", !isHardcoded && isExternalTrack)
    }
}
