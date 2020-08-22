package me.qbosst.bossbot.bot.commands.moderation

import me.qbosst.bossbot.database.data.GuildPunishment
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import java.time.Instant

object WarnCommand : ModerationCommand(
        "warn",
        "Warns a user in the guild",
        aliases = listOf("verbal"),
        type = GuildPunishment.Type.WARN,
        userPermissions = listOf(Permission.MESSAGE_MANAGE, Permission.VOICE_MUTE_OTHERS)
) {
    override fun getRestAction(target: Member, punishment: GuildPunishment): RestAction<*> {
        return target.guild.jda.restPing
    }

    override fun getPunishment(event: MessageReceivedEvent, target: Member, args: List<String>): Pair<RestAction<*>, GuildPunishment>?
    {
        return if(args.isNotEmpty())
        {
            val reason = args.joinToString(" ")
            val p = GuildPunishment.create(target, event.member!!, reason, 0, Instant.now(), type)
            Pair(event.channel.sendMessage("${target.user.asTag} has been warned by ${event.author.asTag} for $reason"), p)
        }
        else
        {
            event.channel.sendMessage("Please provide your reason").queue()
            null
        }
    }

    override fun onSuccessfulPunish(guild: Guild, channel: MessageChannel?, message: Message?, punishment: GuildPunishment)
    {
        punishment.log(guild)
        punishment.getTarget(guild)?.user?.openPrivateChannel()?.flatMap { it.sendMessage("You have been punished for ${punishment.reason}") }?.queue()
    }
}