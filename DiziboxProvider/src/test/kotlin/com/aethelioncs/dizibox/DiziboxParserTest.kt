package com.aethelioncs.dizibox

import org.junit.Assert.*
import org.junit.Test

class DiziboxParserTest {

    @Test
    fun testIsSeriesDetailUrl() {
        assertTrue(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/diziler/loki/"))
        assertTrue(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/diziler/the-ghost-in-the-shell/"))
        assertTrue(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/dizi/friends-izle-hd/"))
        assertTrue(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/friends-izle/"))

        assertFalse(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/arsiv/"))
        assertFalse(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/dizi-takvimi/"))
        assertFalse(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/yardim/"))
        assertFalse(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/iletisim/"))
        assertFalse(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/diziler/"))
        assertFalse(DiziboxParser.isSeriesDetailUrl("https://www.dizibox.live/loki-1-sezon-1-bolum-izle/"))
    }

    @Test
    fun testParseSeasonNumber() {
        assertEquals(1, DiziboxParser.parseSeasonNumber("1.Sezon 5.Bölüm"))
        assertEquals(2, DiziboxParser.parseSeasonNumber("2. Sezon", "https://www.dizibox.live/dizi/2-broke-girls/2-sezon-2-broke-girls/"))
        assertEquals(3, DiziboxParser.parseSeasonNumber("Adults 3. Sezon 1. Bölüm"))
        assertEquals(1, DiziboxParser.parseSeasonNumber("", "https://www.dizibox.live/11-22-63-1-sezon-8-bolum-final-izle/"))
        assertNull(DiziboxParser.parseSeasonNumber("Özel Başlık", "https://www.dizibox.live/ozel-bolum/"))
    }

    @Test
    fun testParseEpisodeNumber() {
        assertEquals(1, DiziboxParser.parseEpisodeNumber("1.Sezon 1.Bölüm"))
        assertEquals(8, DiziboxParser.parseEpisodeNumber("1.Sezon 8.Bölüm Final"))
        assertEquals(12, DiziboxParser.parseEpisodeNumber("The Last of Us (2025) - 12. Bölüm"))
        assertEquals(6, DiziboxParser.parseEpisodeNumber("Bookish 2.Sezon 6.Bölüm Sezon Finali(Killer Nacht - Part 2)"))
        assertEquals(3, DiziboxParser.parseEpisodeNumber("", "https://www.dizibox.live/adults-2-sezon-3-bolum-izle/"))
    }

    @Test
    fun testFixUrl() {
        val mainUrl = "https://www.dizibox.live"
        assertEquals("https://www.dizibox.live/diziler/loki/", DiziboxParser.fixUrl("/diziler/loki/", mainUrl))
        assertEquals("https://cdn.example.com/poster.jpg", DiziboxParser.fixUrl("//cdn.example.com/poster.jpg", mainUrl))
        assertEquals("https://www.dizibox.live/test.html", DiziboxParser.fixUrl("https://www.dizibox.live/test.html", mainUrl))
        assertNull(DiziboxParser.fixUrl("", mainUrl))
    }

    @Test
    fun testCleanPosterUrl() {
        val mainUrl = "https://www.dizibox.live"
        assertEquals(
            "https://www.dizibox.live/wp-content/uploads/afisler/friends-200x290.jpg",
            DiziboxParser.cleanPosterUrl("https://www.dizibox.live/wp-content/uploads/afisler/friends-50x50.jpg", mainUrl)
        )
        assertNull(DiziboxParser.cleanPosterUrl("https://www.dizibox.live/altyazi.png", mainUrl))
    }

    @Test
    fun testParseEpisodesFromHtml() {
        val sampleHtml = """
            <div class="season-episode">
                <a href="https://www.dizibox.live/test-1-sezon-1-bolum-izle/">1.Sezon 1.Bölüm (Pilot)</a>
            </div>
            <div class="season-episode">
                <a href="https://www.dizibox.live/test-1-sezon-2-bolum-izle/">1.Sezon 2.Bölüm (Part 2)</a>
            </div>
        """.trimIndent()

        val episodes = DiziboxParser.parseEpisodes(sampleHtml, defaultSeason = 1, mainUrl = "https://www.dizibox.live")
        assertEquals(2, episodes.size)
        assertEquals(1, episodes[0].season)
        assertEquals(1, episodes[0].episode)
        assertEquals("Pilot", episodes[0].name)
        assertEquals("https://www.dizibox.live/test-1-sezon-1-bolum-izle/", episodes[0].url)

        assertEquals(1, episodes[1].season)
        assertEquals(2, episodes[1].episode)
        assertEquals("Part 2", episodes[1].name)
    }

    @Test
    fun testParseSeasonTabs() {
        val sampleHtml = """
            <a href="https://www.dizibox.live/dizi/sample/1-sezon-sample/" class="btn">1. Sezon</a>
            <a href="https://www.dizibox.live/dizi/sample/2-sezon-sample/" class="btn">2. Sezon</a>
            <a href="https://www.dizibox.live/dizi/sample/3-sezon-sample/" class="btn">3. Sezon</a>
        """.trimIndent()

        val tabs = DiziboxParser.parseSeasonTabs(sampleHtml, mainUrl = "https://www.dizibox.live")
        assertEquals(3, tabs.size)
        assertEquals(1, tabs[0].seasonNumber)
        assertEquals(2, tabs[1].seasonNumber)
        assertEquals(3, tabs[2].seasonNumber)
    }

    @Test
    fun testExtractIframes() {
        val sampleHtml = """
            <iframe src="https://www.dizibox.live/player/king/king.php?v=123" width="100%"></iframe>
            <iframe data-src="https://dbx.molystream.org/embed/456"></iframe>
        """.trimIndent()

        val iframes = DiziboxParser.extractIframes(sampleHtml, "https://www.dizibox.live")
        assertEquals(2, iframes.size)
        assertEquals("https://www.dizibox.live/player/king/king.php?v=123", iframes[0])
        assertEquals("https://dbx.molystream.org/embed/456", iframes[1])
    }
}
