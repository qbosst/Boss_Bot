package qbosst.bossbot.bot.commands.music

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.PlayerPauseEvent
import com.sedmelluq.discord.lavaplayer.player.event.PlayerResumeEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.entities.music.GuildAudioEventListener
import qbosst.bossbot.entities.music.GuildMusicManager
import qbosst.bossbot.util.secondsToString
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

object CurrentTrackCommand : MusicCommand(
        "currenttrack",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
), GuildAudioEventListener {
    private val trackStart = mutableMapOf<Long, TrackInfo>()

    override fun run(event: MessageReceivedEvent, args: List<String>) {
        val manager = GuildMusicManager.get(event.guild)
        val track = manager.currentTrack
        if(track == null)
        {
            event.channel.sendMessage("There is no track currently playing!").queue()
        }
        else
        {
            val current = Duration.between(trackStart[event.guild.idLong]!!.current, Instant.now()).toMillis()
            val total = track.duration

            val percent: Long = (current*100 / track.duration) / 5
            val sb = StringBuilder()
            for(x in 1..20L)
            {
                if(x == percent)
                {
                    sb.append("\\\uD83D\uDD34")
                }
                else
                {
                    sb.append("\u25AC")
                }
            }
            event.channel.sendMessage(EmbedBuilder()
                    .setTitle("Currently Playing")
                    .setDescription("[${track.info.title}](${track.info.uri})")
                    .appendDescription("\n$sb")
                    .appendDescription("\n${secondsToString(current / 1000)} / ${secondsToString(total / 1000)}")
                    .build()).queue()
        }
    }

    fun getCurrent(guild: Guild): Instant?
    {
        return trackStart[guild.idLong]?.current
    }

    override fun onEvent(event: AudioEvent, guildId: Long) {
        when(event)
        {
            is TrackStartEvent ->
            {
                trackStart[guildId] = TrackInfo(Instant.now())
            }

            is PlayerPauseEvent ->
            {
                trackStart[guildId]?.update(Instant.now())
            }
            is PlayerResumeEvent ->
            {
                trackStart[guildId]?.update(null)
            }
        }
    }

    data class TrackInfo(
            private val start: Instant
    )
    {
        var pause: Instant? = null
        var millisPaused: Long = 0L

        val current: Instant
            get() = start.plusMillis(millisPaused).plusMillis(if(pause != null) Duration.between(pause, OffsetDateTime.now()).toMillis() else 0L)

        fun update(pause: Instant?)
        {
            if(this.pause != null)
            {
                millisPaused += Duration.between(this.pause, OffsetDateTime.now()).toMillis()
            }
            this.pause = pause
        }

    }

}