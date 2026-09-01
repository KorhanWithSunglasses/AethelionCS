package com.aethelioncs.dizibox

data class ParsedEpisode(
    val name: String,
    val season: Int,
    val episode: Int,
    val url: String
)

data class SeasonTab(
    val seasonNumber: Int,
    val url: String
)

data class DiziboxSearchItem(
    val title: String,
    val url: String,
    val posterUrl: String? = null
)
