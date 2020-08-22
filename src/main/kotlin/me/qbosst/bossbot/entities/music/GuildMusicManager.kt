package me.qbosst.bossbot.entities.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.util.loadClasses
import me.qbosst.bossbot.util.makeSafe
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue


class GuildMusicManager private constructor(
        manager: AudioPlayerManager
): AudioEventListener
{
    private val queue = LinkedBlockingQueue<AudioTrack>()
    private val player = manager.createPlayer()
    private val listeners = mutableSetOf<GuildAudioEventListener>()

    private var loop: Boolean = false

    private var lastMessageIdSent: Long = 0L
    private var lastChannelIdSent: Long = 0L

    val currentTrack: AudioTrack?
        get() = player.playingTrack

    var paused: Boolean
        get() = player.isPaused
        set(value)
        {
            player.isPaused = value
        }

    init
    {
        player.addListener(this)
        loadClasses("me.qbosst.bossbot.bot.commands.music", GuildAudioEventListener::class.java).forEach { listeners.add(it) }
    }

    fun getSendHandler(): AudioSendHandler
    {
        return object : AudioSendHandler
        {
            private val buffer: ByteBuffer = ByteBuffer.allocate(1024)
            private val frame: MutableAudioFrame = MutableAudioFrame()

            init
            {
                frame.setBuffer(buffer)
            }

            override fun canProvide(): Boolean
            {
                return player.provide(frame)
            }

            override fun provide20MsAudio(): ByteBuffer?
            {
                return (buffer as Buffer).flip() as ByteBuffer
            }

            override fun isOpus(): Boolean
            {
                return true
            }
        }

    }

    fun loadAndPlay(message: Message, trackUrl: String, handler: AudioLoadResultHandler =
            object : AudioLoadResultHandler
            {
                override fun trackLoaded(track: AudioTrack) {
                    queue(track)
                    if(currentTrack != track)
                    {
                        message.editMessage("Added to queue: `${track.info.title}`").queue()
                    }
                    else
                    {
                        message.delete().queue()
                    }
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {
                    playlist.tracks.forEach { queue(it) }
                    message.editMessage("Added `${playlist.tracks.size}` songs from playlist `${playlist.name}`").queue()
                }

                override fun noMatches() {
                    message.editMessage("I could not find anything from `${trackUrl.makeSafe()}`").queue()
                }

                override fun loadFailed(exception: FriendlyException) {
                    message.editMessage("Could not play track: `${exception.message}`").queue()
                }
            })
    {
        if(lastChannelIdSent != message.channel.idLong)
        {
            if(lastMessageIdSent != 0L)
            {
                message.guild.getTextChannelById(lastChannelIdSent)?.deleteMessageById(lastMessageIdSent)?.queue()
                {
                    lastMessageIdSent = 0L
                }
            }
            lastChannelIdSent = message.channel.idLong
        }
        playerManager.loadItemOrdered(this, trackUrl, handler)
    }

    fun queue(track: AudioTrack)
    {
        if(!player.startTrack(track, true))
        {
            queue.offer(track)
        }
    }

    fun nextTrack()
    {
        player.startTrack(queue.poll(), false)
    }

    fun getQueue(includeCurrentTrack: Boolean = true): List<AudioTrack> {
        val queue = queue.toMutableList()
        if(includeCurrentTrack && currentTrack != null)
        {
            queue.add(0, currentTrack)
        }
        return queue
    }

    fun removeTrack(index: Int): AudioTrack?
    {
        val track = getQueue().getOrNull(index)
        if(track != null)
        {
            if(currentTrack == track)
            {
                player.isPaused = false
                nextTrack()
            }
            queue.remove(track)
        }
        return track
    }

    fun reverse()
    {
        if(queue.size > 1)
        {
            val reversed = queue.reversed()
            queue.clear()
            for(track in reversed)
            {
                queue(track)
            }
        }
    }

    fun loop(value: Boolean = !loop): Boolean
    {
        loop = value
        return loop
    }

    fun shuffle()
    {
        if(queue.size > 1)
        {
            val shuffled = queue.shuffled()
            queue.clear()
            for(track in shuffled)
            {
                queue(track)
            }
        }
    }

    override fun onEvent(event: AudioEvent) {
        val guildId: Long = getGuildId(this)
        when(event)
        {
            is TrackEndEvent ->
            {
                if(event.endReason.mayStartNext)
                {
                    nextTrack()
                }
                else if(event.endReason == AudioTrackEndReason.STOPPED)
                {
                    val channel = BossBot.shards.getGuildById(guildId)?.getTextChannelById(lastChannelIdSent)
                    channel?.deleteMessageById(lastMessageIdSent)?.queue(
                            {
                                lastMessageIdSent = 0L
                            },
                            {
                                lastMessageIdSent = 0L
                            })
                }

                if(event.endReason != AudioTrackEndReason.LOAD_FAILED && loop)
                {
                    queue(event.track.makeClone())
                }
            }

            is TrackStartEvent ->
            {
                val channel = BossBot.shards.getTextChannelById(lastChannelIdSent)
                if(channel != null)
                {
                    if(lastMessageIdSent != 0L)
                    {
                        channel.deleteMessageById(lastMessageIdSent).queue({}, {})
                    }
                    channel.sendMessage("Now playing: ${event.track.info.title}").queue()
                    {
                        lastMessageIdSent = it.idLong
                    }
                }
            }
        }
        listeners.forEach { it.onEvent(event, guildId) }
    }

    companion object
    {
        private val playerManager = DefaultAudioPlayerManager()
        private val map = mutableMapOf<Long, GuildMusicManager>()

        init
        {
            AudioSourceManagers.registerRemoteSources(playerManager)
            AudioSourceManagers.registerLocalSource(playerManager)
        }

        fun get(guild: Guild): GuildMusicManager
        {
            val manager = if(map.containsKey(guild.idLong))
            {
                map[guild.idLong]!!
            }
            else
            {
                val manager = GuildMusicManager(playerManager)
                map[guild.idLong] = manager
                manager
            }

            guild.audioManager.sendingHandler = manager.getSendHandler()
            return manager
        }

        fun remove(guild: Guild): GuildMusicManager?
        {
            map[guild.idLong]?.player?.destroy()
            return map.remove(guild.idLong)
        }

        private fun getGuildId(manager: GuildMusicManager): Long
        {
            return map.keys.first { map[it] == manager }
        }
    }
}