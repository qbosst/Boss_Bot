package me.qbosst.bossbot.entities.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.*
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import java.util.concurrent.LinkedBlockingQueue

class TrackScheduler(
        private val player: AudioPlayer,

): GuildAudioEventListener
{
    private val queue = LinkedBlockingQueue<AudioTrack>()
    private val reloadCount = mutableMapOf<String, Int>()
    private var messageId: Long = -1
    private lateinit var lastException: FriendlyException
    var channelId: Long = -1

    val channel: TextChannel?
        get() = BossBot.SHARDS_MANAGER.getTextChannelById(channelId)

    val currentTrack: AudioTrack?
        get() = player.playingTrack

    var paused: Boolean
        get() = player.isPaused
        set(value) { player.isPaused = value }

    var isLooping: Boolean = false

    fun queue(track: AudioTrack)
    {
        if(!player.startTrack(track, true))
            queue.offer(track)
    }

    fun nextTrack() = player.startTrack(queue.poll(), false)

    fun getQueue(includeCurrentTrack: Boolean = true): List<AudioTrack>
    {
        val queue = queue.toMutableList()
        if(includeCurrentTrack && currentTrack != null)
            queue.add(0, currentTrack)
        return queue
    }

    fun removeTrack(index: Int): AudioTrack?
    {
        val track = getQueue().getOrNull(index)
        if(track != null)
        {
            if(currentTrack == track)
                nextTrack()
            queue.remove(track)
        }
        return track
    }

    fun reverse()
    {
        val reversed = queue.reversed()
        queue.clear()
        for(track in reversed)
            queue(track)
    }

    fun shuffle()
    {
        val shuffled = queue.shuffled()
        queue.clear()
        for(track in shuffled)
            queue(track)
    }

    fun loadAndPlay(message: Message, trackUrl: String)
    {
        channelId = message.channel.idLong

        GuildMusicManager.audioManager.loadItemOrdered(player, trackUrl, object: AudioLoadResultHandler
        {
            override fun trackLoaded(track: AudioTrack)
            {
                queue(track)
                message.editMessage("Added to queue: `${track.info.title}`").queue()
                reloadCount.remove(trackUrl)
            }

            override fun playlistLoaded(playlist: AudioPlaylist)
            {
                for(track in playlist.tracks)
                    queue(track)
                message.editMessage("Added `${playlist.tracks.size}` songs from the playlist `${playlist.name}`").queue()
            }

            override fun noMatches()
            {
                message.editMessage("Nothing found by: `$trackUrl`").queue()
            }

            override fun loadFailed(exception: FriendlyException)
            {
                // Sometimes there are problems with loading tracks that can be solved by just trying again
                // This will try 5 to load the track 5 times before it fails
                if(reloadCount.getOrDefault(trackUrl, 0) < 5)
                {
                    reloadCount[trackUrl] = reloadCount.getOrDefault(trackUrl, 0)+1
                    loadAndPlay(message, trackUrl)
                }
                else
                {
                    reloadCount.remove(trackUrl)
                    message.editMessage("Could not play: `${exception.localizedMessage.maxLength(256)}`").queue()
                }
            }
        })
    }

    override fun onEvent(manager: GuildMusicManager, event: AudioEvent)
    {
        when(event)
        {
            is TrackExceptionEvent ->
                lastException = event.exception

            is TrackEndEvent ->
            {
                if(event.endReason == AudioTrackEndReason.LOAD_FAILED)
                    channel?.editMessageById(messageId, "Failed to play track `${event.track.info.title}`: ${lastException.localizedMessage}")?.queue() { messageId = -1 }

                // Deletes the old 'currently playing' message
                else if(messageId != -1L)
                    channel?.deleteMessageById(messageId)?.queue() { messageId = -1 }

                // Starts the next track
                if(event.endReason.mayStartNext)
                    nextTrack()

                // Adds the track to the queue again if looping
                if(isLooping && event.endReason != AudioTrackEndReason.LOAD_FAILED)
                    queue(event.track.makeClone())
            }

            is TrackStartEvent ->
                // Displays the currently playing track
                channel?.sendMessage("Now playing: `${event.track.info.title}`")?.queue() { messageId = it.idLong }

            is TrackStuckEvent ->
                // Starts the next track
                nextTrack()

        }
    }
}