package me.qbosst.bossbot.entities.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.entities.music.source.spotify.SpotifyAudioSourceManager
import me.qbosst.bossbot.entities.music.source.youtube.YoutubeModifiedAudioSourceManager
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Guild

class GuildMusicManager(guild: Guild, manager: AudioPlayerManager)
{
    private val player: AudioPlayer = manager.createPlayer()
    val scheduler = TrackScheduler(player)
    val guildId = guild.idLong

    init
    {
        player.addListener(scheduler)
    }
    
    fun getSendHandler(): AudioSendHandler = AudioPlayerSendHandler(player)

    companion object
    {
        val audioManager = DefaultAudioPlayerManager()
        private val playerManager = mutableMapOf<Long, GuildMusicManager>()

        init {
            // add spotify manager
            audioManager.registerSourceManager(SpotifyAudioSourceManager(
                    clientId = BotConfig.spotify_client_id,
                    clientSecret = BotConfig.spotify_client_secret
            ))
            audioManager.registerSourceManager(YoutubeModifiedAudioSourceManager())

            AudioSourceManagers.registerRemoteSources(audioManager)
            AudioSourceManagers.registerLocalSource(audioManager)
        }

        fun get(guild: Guild): GuildMusicManager
        {
            if(playerManager.containsKey(guild.idLong))
                return playerManager[guild.idLong]!!

            val manager = GuildMusicManager(guild, audioManager)
            playerManager[guild.idLong] = manager

            guild.audioManager.sendingHandler = manager.getSendHandler()
            return manager
        }

        fun remove(guild: Guild): GuildMusicManager?
        {
            val manager = playerManager.remove(guild.idLong)
            manager?.player?.destroy()
            return manager
        }
    }

}