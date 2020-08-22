package qbosst.bossbot.bot.commands.moderation

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import qbosst.bossbot.database.data.GuildPunishment
import java.time.Instant

object KickCommand : ModerationCommand(
        "kick",
        "Kicks a user from the guild",
        userPermissions = listOf(Permission.KICK_MEMBERS),
        botPermissions = listOf(Permission.KICK_MEMBERS),
        type = GuildPunishment.Type.KICK
) {
    override fun getRestAction(target: Member, punishment: GuildPunishment): RestAction<*> {
        return target.kick()
    }

    override fun getPunishment(event: MessageReceivedEvent, target: Member, args: List<String>): Pair<RestAction<*>, GuildPunishment>?
    {
        return if(args.isNotEmpty())
        {
            val reason = args.joinToString(" ")

            Pair(event.guild.kick(target).reason(reason), GuildPunishment.create(target, event.member!!, reason, 0, Instant.now(), type))
        }
        else
        {
            event.channel.sendMessage("Please provide the reason you would like to kick them for!")
            null
        }
    }
}