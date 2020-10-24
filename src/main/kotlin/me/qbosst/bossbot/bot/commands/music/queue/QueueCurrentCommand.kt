package me.qbosst.bossbot.bot.commands.music.queue

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.PlayerPauseEvent
import com.sedmelluq.discord.lavaplayer.player.event.PlayerResumeEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent
import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildAudioEventListener
import me.qbosst.bossbot.entities.music.GuildMusicManager
import me.qbosst.bossbot.util.TimeUtil
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.Duration
import java.time.Instant

object QueueCurrentCommand: MusicCommand(
        "current",
        description = "Shows the currently playing track"
), GuildAudioEventListener
{

    private val trackTime = mutableMapOf<Long, CurrentTrackInfo>()

    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val manager = GuildMusicManager.get(event.guild)
        val track = manager.scheduler.currentTrack
        if(track == null)
            event.channel.sendMessage("There is no track currently playing").queue()
        else
        {
            val millisPlayed = trackTime[event.guild.idLong]!!.millisPlayed
            val total = track.duration

            val percent: Long = (millisPlayed*100 / total) / 5

            val sb = StringBuilder()
            for(x in 0..20L)
                if(x == percent)
                    sb.append("\\\uD83D\uDD34")
                else
                    sb.append("\u25AC")

            event.channel.sendMessage(EmbedBuilder()
                    .setTitle("Currently Playing")
                    .appendDescription("[${track.info.title}](${track.info.uri})\n$sb\n")
                    .appendDescription("${TimeUtil.secondsToString(millisPlayed/1000)} / ${TimeUtil.secondsToString(total/1000)}")
                    .setColor(event.guild.selfMember.colorRaw)
                    .build()
            ).queue()
        }
    }

    override fun onEvent(manager: GuildMusicManager, event: AudioEvent)
    {
        when(event)
        {
            is PlayerPauseEvent ->
                trackTime[manager.guildId]?.update(Instant.now())
            is PlayerResumeEvent ->
                trackTime[manager.guildId]?.update(null)
            is TrackStartEvent ->
                trackTime[manager.guildId] = CurrentTrackInfo(Instant.now(), event.track.duration)
        }
    }

    /**
     *  Class used to keep track of how long a track has been playing for and where it is in the track right now
     *
     *  @param start The start time of when the track was played
     */
    private data class CurrentTrackInfo(
            private var start: Instant,
            private val totalMillis: Long,
    )
    {
        var pauseTime: Instant? = null
            private set

        var millisPaused: Long = 0
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
}