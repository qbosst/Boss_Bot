package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.util.TimeUtil
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.math.roundToInt

object QueueCurrentCommand: MusicCommand(
        "current",
        "Shows information about the currently playing track",
        requiresMemberConnected = false
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val handler = event.guild.getAudioHandler()
        val track = handler.playingTrack
        if(track == null)
            event.channel.sendMessage("There is no track currently playing").queue()
        else
        {
            val millisLeft = track.duration-track.position
            val percent = (track.position*100/track.duration).coerceIn(0, 100)

            val sb = StringBuilder()
            for(x in 0..20L)
                if(x == percent/5)
                    sb.append("\\\uD83D\uDD34")
                else
                    sb.append("\u25AC")

            event.channel.sendMessage(EmbedBuilder()
                    .setTitle("Currently Playing")
                    .appendDescription("[${track.info.title}](${track.info.uri})\n$sb\n")
                    .appendDescription("${TimeUtil.secondsToString(((track.duration-millisLeft).toFloat()/1000).roundToInt())} / ${TimeUtil.secondsToString(track.duration/1000)}")
                    .setColor(event.guild.selfMember.colorRaw)
                    .build()
            ).queue()
        }
    }
}