package me.qbosst.bossbot.entities.music

import java.time.Duration
import java.time.Instant

/**
 *  Class used to keep track of how long a track has been playing for and where it is in the track right now
 *
 *  @param start The start time of when the track was played
 */
data class CurrentTrackInfo(
    private val start: Instant,
    private val totalMillis: Long,
)
{
    var pauseTime: Instant? = null
        private set

    var millisPaused: Long = 0L
        private set

    val millisPlayed: Long
        get() {
            val current = start.plusMillis(millisPaused).plusMillis(if(pauseTime != null) Duration.between(pauseTime, Instant.now()).toMillis() else 0L)
            return Duration.between(current, Instant.now()).toMillis()
        }

    val millisLeft: Long
        get() = (totalMillis - millisPlayed)

    fun update(pauseTime: Instant?)
    {
        if(this.pauseTime != null)
            millisPaused += Duration.between(this.pauseTime, Instant.now()).toMillis()
        this.pauseTime = pauseTime
    }
}