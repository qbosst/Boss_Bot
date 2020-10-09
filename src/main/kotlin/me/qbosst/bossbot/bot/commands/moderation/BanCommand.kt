package me.qbosst.bossbot.bot.commands.moderation

import me.qbosst.bossbot.entities.database.GuildPunishment
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.requests.RestAction
import java.time.Instant

object BanCommand: ModerationCommand(
        "ban",
        userPermissions = listOf(Permission.BAN_MEMBERS),
        botPermissions = listOf(Permission.BAN_MEMBERS)
)
{
    override fun getRestAction(target: Member): RestAction<*>
    {
        return target.ban(0)
    }

    override fun getPunishment(target: Member, issuer: Member, args: List<String>): GuildPunishment
    {
        return GuildPunishment(
            targetId = target.idLong,
            issuerId = issuer.idLong,
            reason = if(args.isNotEmpty()) args.joinToString(" ") else null,
            duration = 0,
            date = Instant.now(),
            type = GuildPunishment.Type.BAN
        )
    }
}