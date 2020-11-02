package me.qbosst.bossbot.entities.music.source.spotify

data class SpotifyTrack(
        val artists: List<String>,
        val name: String,
        val uri: String,
        val durationMs: Int,
        val id: String
)