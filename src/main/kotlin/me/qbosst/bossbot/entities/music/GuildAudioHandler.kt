package me.qbosst.bossbot.entities.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Guild
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

class GuildAudioHandler private constructor(
        private val player: AudioPlayer
): AudioSendHandler, AudioEventListener
{

    private var lastFrame: AudioFrame? = null
    private val queue = LinkedBlockingQueue<AudioTrack>()

    /**
     * The currently playing track on the bot. Null if no track is currently playing
     */
    val playingTrack: AudioTrack?
        get() = player.playingTrack

    /**
     * Determines whether the current playing track is paued or not
     */
    var paused: Boolean
        get() = player.isPaused
        set(value) {
            player.isPaused = value
        }

    /**
     * Determines whether the track gets put to the back of the queue when finished.
     */
    var isLooping: Boolean = false


    init {
        player.addListener(this)
    }

    override fun canProvide(): Boolean
    {
        lastFrame = player.provide()
        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteBuffer? = ByteBuffer.wrap(lastFrame?.data)

    override fun isOpus(): Boolean = true

    // Track Scheduler Methods

    /**
     * Queues a track on the track queue
     *
     * @param track The track to queue
     */
    fun queue(track: AudioTrack)
    {
        if(!player.startTrack(track, true))
            queue.offer(track)
    }

    /**
     * Force starts the next track.
     */
    fun nextTrack() = player.startTrack(queue.poll(), false)

    fun getQueue(includeCurrentlyPlaying: Boolean = true): List<AudioTrack>
    {
        val queue = queue.toMutableList()
        if(includeCurrentlyPlaying && player.playingTrack != null)
            queue.add(0, player.playingTrack.makeClone())
        return queue
    }

    /**
     * Removes a track from the queue. If the track removed is the currently playing one it will skip it and play
     * the next track on the queue
     *
     * @param index The index of the track to remove
     *
     * @return The audio track that has been removed from the queue. Null if no track was at the index position given
     */
    fun removeTrack(index: Int): AudioTrack?
    {
        val track = getQueue().getOrNull(index)
        if(track != null)
        {
            if(playingTrack == track)
                nextTrack()
            queue.remove(track)
        }
        return track
    }

    /**
     * Reverses the queue
     *
     * @param reverseCurrent Whether to also reverse the currently playing song. If true, this will stop it, reverse it
     * and then play the next track on the new reversed queue. False by default
     *
     */
    fun reverse(reverseCurrent: Boolean = false)
    {
        val reversed = getQueue(reverseCurrent).reversed()
        queue.clear()
        for(track in reversed)
            queue(track)

        if(reverseCurrent && player.playingTrack != null)
            nextTrack()
    }

    /**
     * Shuffles the queue
     *
     * @param shuffleCurrent Whether to also shuffle the currently playing song. If true, this will stop it, shuffle it
     * and then play the next track on the new shuffled queue. False by default.
     */
    fun shuffle(shuffleCurrent: Boolean = false)
    {
        val shuffled = getQueue(shuffleCurrent).shuffled()
        queue.clear()
        for(track in shuffled)
            queue(track)

        if(shuffleCurrent && player.playingTrack != null)
            nextTrack()
    }

    fun clear(includeCurrent: Boolean = false)
    {
        queue.clear()
        if(includeCurrent)
        {
            if(isLooping)
            {
                isLooping = false
                nextTrack()
                isLooping = true
            }
            else
                nextTrack()
        }
    }

    /**
     * Loads and queues the track from the query provided
     *
     * @param query The query to try and search the track by
     * @param result The handler for the result of the query
     */
    fun loadAndPlay(query: String, result: AudioLoadResultHandler)
    {
        manager.loadItemOrdered(player, query, result)
    }

    override fun onEvent(event: AudioEvent)
    {
        when(event)
        {
            is TrackEndEvent ->
            {
                if(event.endReason.mayStartNext)
                    nextTrack()

                if(isLooping && event.endReason != AudioTrackEndReason.LOAD_FAILED)
                    queue(event.track.makeClone())
            }

            is TrackStuckEvent ->
                nextTrack()
        }
    }

    companion object
    {
        private val manager = PlayerManager()

        fun get(guild: Guild): GuildAudioHandler
        {
            return if(guild.audioManager.sendingHandler == null)
            {
                val player = manager.createPlayer()
                val handler = GuildAudioHandler(player)

                guild.audioManager.sendingHandler = handler

                handler
            }
            else
                guild.audioManager.sendingHandler as GuildAudioHandler
        }

        fun destroy(guild: Guild): GuildAudioHandler?
        {
            val handler = (guild.audioManager.sendingHandler ?: return null) as GuildAudioHandler

            handler.queue.clear()
            handler.player.destroy()
            guild.audioManager.sendingHandler = null

            return handler
        }
    }
}
