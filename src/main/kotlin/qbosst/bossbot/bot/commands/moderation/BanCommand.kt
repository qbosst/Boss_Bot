package qbosst.bossbot.bot.commands.moderation

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.requests.RestAction
import qbosst.bossbot.bot.BossBot
import qbosst.bossbot.database.data.GuildPunishment
import qbosst.bossbot.database.data.GuildSettingsData
import java.time.Instant
import java.util.concurrent.TimeUnit

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