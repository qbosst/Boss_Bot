package me.qbosst.bossbot.bot.commands.economy

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.listeners.Listener
import me.qbosst.bossbot.bot.userNotFound
import me.qbosst.bossbot.entities.database.GuildUserData
import me.qbosst.bossbot.util.getMemberByString
import me.qbosst.bossbot.util.secondsToString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object StatsCommand : Command(
        "stats",
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
        val stats = GuildUserData.get(member)

        return EmbedBuilder()
                .addField("Messages Sent", (stats.message_count + Listener.getCachedMessageCount(member)).toString(), true)
                .addField("Text Chat Time", secondsToString(stats.text_chat_time), true)
                .addField("Voice Chat Time", secondsToString(stats.voice_chat_time + Listener.getCachedVoiceChatTime(member)), true)
                .setColor(member.colorRaw)
    }
}