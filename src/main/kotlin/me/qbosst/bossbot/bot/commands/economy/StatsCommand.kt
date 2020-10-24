package me.qbosst.bossbot.bot.commands.economy

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.listeners.MessageListener
import me.qbosst.bossbot.bot.listeners.VoiceListener
import me.qbosst.bossbot.bot.userNotFound
import me.qbosst.bossbot.database.managers.getMemberData
import me.qbosst.bossbot.util.TimeUtil
import me.qbosst.bossbot.util.getMemberByString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object StatsCommand : Command(
        "stats",
        description = "Shows a member's stats",
        usage_raw = listOf("[@user]"),
        examples_raw = listOf("", "@boss"),
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
) {
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val target: Member = if(args.isNotEmpty()) {
            val name = args.joinToString(" ")
            event.guild.getMemberByString(name) ?: kotlin.run {
                event.channel.sendMessage(userNotFound(name)).queue()
                return
            }
        } else event.member!!

        event.channel.sendMessage(statsEmbed(target).build()).queue()
    }

    private fun statsEmbed(member: Member): EmbedBuilder
    {
        val stats = member.getMemberData()

        return EmbedBuilder()
                .addField("Messages Sent", (stats.message_count + MessageListener.getCachedMessageCount(member)).toString(), true)
                .addField("Text Chat Time", TimeUtil.secondsToString(stats.text_chat_time), true)
                .addField("Voice Chat Time", TimeUtil.secondsToString(stats.voice_chat_time + VoiceListener.getCachedVoiceChatTime(member)), true)
                .setColor(member.colorRaw)
    }
}