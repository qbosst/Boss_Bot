package me.qbosst.bossbot.bot.commands.moderation

import me.qbosst.bossbot.entities.database.GuildPunishment
import me.qbosst.fbiagent.bot.commands.moderation.ModerationCommand
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.requests.RestAction
import java.time.Instant

object KickCommand: ModerationCommand(
    "kick"
) {
    override fun getRestAction(target: Member): RestAction<*> {
        return target.kick()
    }

    override fun getPunishment(target: Member, issuer: Member, args: List<String>): GuildPunishment {
        return GuildPunishment(
            targetId = target.idLong,
            issuerId = issuer.idLong,
            reason = if(args.isNotEmpty()) args.joinToString(" ") else null,
            duration = 0,
            date = Instant.now(),
            type = GuildPunishment.Type.KICK
        )
    }
}