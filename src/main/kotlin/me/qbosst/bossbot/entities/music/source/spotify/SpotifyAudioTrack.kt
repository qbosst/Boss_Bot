package me.qbosst.bossbot.entities.music.source.spotify

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.*
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor

class SpotifyAudioTrack(trackInfo: AudioTrackInfo,
                        private val sourceManager: YoutubeAudioSourceManager
): DelegatedAudioTrack(trackInfo)
{
    /**
     * Track used to play audio from. This is not initialized until it needs to be processed to play from
     */
    private lateinit var ytTrack: YoutubeAudioTrack

    override fun process(executor: LocalAudioTrackExecutor)
    {
        // Creates the query to search videos from youtube
        val query = "ytsearch:${trackInfo.title} ${trackInfo.author}"

        // Get list of tracks found from query
        when(val results = sourceManager.loadItem(null, AudioReference(query, query))) {
            null ->
                throw FriendlyException("Could not find youtube video from identifier", FriendlyException.Severity.COMMON, null)
            is BasicAudioPlaylist -> {
                // Select first one, if no tracks received send an exception
                val track = results.tracks.firstOrNull()
                        ?: throw FriendlyException("Could not find youtube video from identifier",
                                FriendlyException.Severity.COMMON, null)

                // Play the track
                ytTrack = YoutubeAudioTrack(track.info, sourceManager)
                ytTrack.userData = this.userData
                ytTrack.process(executor)
            }
            else ->
                throw FriendlyException("Received unhandled result type (${results})", FriendlyException.Severity.FAULT, null)
        }
    }

    override fun getDuration(): Long = if(this::ytTrack.isInitialized) ytTrack.duration else super.getDuration()

    override fun getInfo(): AudioTrackInfo = if(this::ytTrack.isInitialized) ytTrack.info else super.getInfo()

    override fun getIdentifier(): String = if(this::ytTrack.isInitialized) ytTrack.identifier else super.getIdentifier()

    override fun makeClone(): AudioTrack? = if(this::ytTrack.isInitialized) ytTrack.makeClone() else SpotifyAudioTrack(trackInfo, sourceManager)
}