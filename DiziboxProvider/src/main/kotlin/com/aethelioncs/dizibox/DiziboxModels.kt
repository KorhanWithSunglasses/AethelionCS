package com.aethelioncs.dizibox

/**
 * Lightweight discovery model for candidate media links / embeds.
 */
data class SourceCandidate(
    val url: String,
    val sourceName: String = "DiziBox",
    val host: String? = null,
    val originChain: String? = null
)

/**
 * Parsed season information with its episode fetch URL.
 */
data class SeasonTab(
    val seasonNumber: Int,
    val url: String
)

/**
 * Parsed episode metadata from series or season page.
 */
data class ParsedEpisode(
    val name: String?,
    val season: Int,
    val episode: Int,
    val url: String,
    val posterUrl: String? = null,
    val date: String? = null
)
