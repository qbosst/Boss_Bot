package me.qbosst.bossbot.bot.commands.moderation

import me.qbosst.bossbot.database.data.GuildPunishment
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import java.time.Instant

object BanCommand: ModerationCommand(
        "ban",
        "Bans a member from the guild",
        listOf("<reason>"),
        listOf("spam", "toxicity"),
        userPermissions = listOf(Permission.BAN_MEMBERS),
        botPermissions = listOf(Permission.BAN_MEMBERS),
        type = GuildPunishment.Type.BAN
) {

    override fun getRestAction(target: Member, punishment: GuildPunishment): RestAction<*> {
        return target.ban(0, punishment.reason)
    }

    override fun getPunishment(event: MessageReceivedEvent, target: Member, args: List<String>): Pair<RestAction<*>, GuildPunishment>?
    {
        return if(args.isNotEmpty())
        {
            val reason = args.joinToString(" ")
            Pair(event.guild.ban(target, 0), GuildPunishment.create(target, event.member!!, reason, 0, Instant.now(), type))
        }
        else
        {
            event.channel.sendMessage("Please provide the reason you would like to ban them for!")
            null
        }
    }

}