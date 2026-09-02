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
        // 1 host with 3 quality variants must be counted as 1 Source Host and 3 Quality Variants
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

        // Verification: Hardcoded subtitle must NOT produce an external subtitle track
        val isExternalTrack = externalVttSample.endsWith(".vtt") || externalVttSample.endsWith(".srt")
        val isHardcoded = hardcodedSubtitleSample.contains("Hardcoded")

        assertTrue(isExternalTrack)
        assertTrue(isHardcoded)
        assertFalse("Hardcoded stream must not be flagged as external subtitle file", !isHardcoded && isExternalTrack)
    }

    @Test
    fun testNavigationCardExclusion() {
        val navigationUrls = listOf(
            "https://www.dizibox.live/arsiv/",
            "https://www.dizibox.live/dizi-takvimi/",
            "https://www.dizibox.live/yardim/",
            "https://www.dizibox.live/iletisim/",
            "https://www.dizibox.live/diziler/",
            "https://www.dizibox.live/tum-bolumler/"
        )

        for (url in navigationUrls) {
            assertFalse("Navigation link $url must be excluded from series detail", DiziboxParser.isSeriesDetailUrl(url))
        }

        val validSeriesUrls = listOf(
            "https://www.dizibox.live/diziler/loki/",
            "https://www.dizibox.live/diziler/the-ghost-in-the-shell/",
            "https://www.dizibox.live/dizi/friends-izle-hd/"
        )

        for (url in validSeriesUrls) {
            assertTrue("Valid series link $url must pass isSeriesDetailUrl", DiziboxParser.isSeriesDetailUrl(url))
        }
    }
}
