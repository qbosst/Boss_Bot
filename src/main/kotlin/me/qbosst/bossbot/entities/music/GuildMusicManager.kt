package me.qbosst.bossbot.entities.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory

class GuildMusicManager(guild: Guild, manager: AudioPlayerManager): AudioEventListener
{
    private val listeners = mutableSetOf<GuildAudioEventListener>()

    private val player: AudioPlayer = manager.createPlayer()
    val scheduler = TrackScheduler(player)
    val guildId = guild.idLong

    init
    {
        player.addListener(this)
        listeners.add(scheduler)
    }
    
    fun getSendHandler(): AudioSendHandler = AudioPlayerSendHandler(player)

    fun addListener(listener: GuildAudioEventListener) = listeners.add(listener)

    override fun onEvent(event: AudioEvent)
    {
        for(listener in listeners)
            try
            {
                listener.onEvent(this, event)
            }
            catch (t: Throwable)
            {
                LOG.error("Caught exception:", t)
            }
    }

    companion object
    {
        private val LOG = LoggerFactory.getLogger(this::class.java)

        val audioManager = DefaultAudioPlayerManager()
        private val playerManager = mutableMapOf<Long, GuildMusicManager>()

        init {
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