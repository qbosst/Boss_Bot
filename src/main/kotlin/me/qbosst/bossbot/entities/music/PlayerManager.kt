package me.qbosst.bossbot.entities.music

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.*
import com.wrapper.spotify.SpotifyApi
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.entities.music.source.spotify.SpotifyAudioSourceManager
import me.qbosst.bossbot.entities.music.source.youtube.YoutubeLinkRouter

class PlayerManager: DefaultAudioPlayerManager()
{
    init {
        registerSources(MediaContainerRegistry.DEFAULT_REGISTRY)
    }

    private fun registerSources(registry: MediaContainerRegistry)
    {
        val youtube = YoutubeAudioSourceManager(
                true,
                DefaultYoutubeTrackDetailsLoader(),
                YoutubeSearchProvider(),
                YoutubeSignatureCipherManager(),
                DefaultYoutubePlaylistLoader(),
                YoutubeLinkRouter(),
                YoutubeMixProvider()
        )

        // Load Spotify. This needs to be registered before youtube.
        this.registerSourceManager(SpotifyAudioSourceManager(
                SpotifyApi.Builder()
                        .setClientId(BotConfig.spotify_client_id)
                        .setClientSecret(BotConfig.spotify_client_secret)
                        .build(),
                youtube
        ))

        // Load Youtube
        this.registerSourceManager(youtube)

        // Others
        this.registerSourceManager(SoundCloudAudioSourceManager.createDefault())
        this.registerSourceManager(BandcampAudioSourceManager())
        this.registerSourceManager(VimeoAudioSourceManager())
        this.registerSourceManager(TwitchStreamAudioSourceManager())
        this.registerSourceManager(BeamAudioSourceManager())
        this.registerSourceManager(GetyarnAudioSourceManager())
        this.registerSourceManager(HttpAudioSourceManager(registry))

        this.registerSourceManager(LocalAudioSourceManager(registry))
    }
}