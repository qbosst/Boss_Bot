package me.qbosst.bossbot.bot.commands.music.queue

import me.qbosst.bossbot.bot.commands.music.MusicCommand
import me.qbosst.bossbot.entities.music.GuildMusicManager
import me.qbosst.bossbot.util.TimeUtil
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object QueueCurrentCommand: MusicCommand(
        "current",
        description = "Shows the currently playing track"
)
{

    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        val manager = GuildMusicManager.get(event.guild)
        val track = manager.scheduler.currentTrack
        if(track == null)
            event.channel.sendMessage("There is no track currently playing").queue()
        else
        {
            val millisPlayed = manager.scheduler.currentTrackInfoInfo!!.millisPlayed
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
}