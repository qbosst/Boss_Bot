package me.qbosst.bossbot.bot.commands.moderation

import me.qbosst.bossbot.bot.exception.MissingArgumentException
import me.qbosst.bossbot.entities.database.GuildPunishment
import me.qbosst.bossbot.util.getSeconds
import me.qbosst.fbiagent.bot.commands.moderation.ModerationCommand
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.requests.RestAction
import java.time.Instant

object TempbanCommand: ModerationCommand(
        "tempban"
) {

    override fun getPunishment(target: Member, issuer: Member, args: List<String>): GuildPunishment {
        if(args.isNotEmpty())
        {
            val seconds = getSeconds(args[0])
            if(seconds > 60)
            {
                return GuildPunishment(
                        targetId = target.idLong,
                        issuerId = issuer.idLong,
                        reason = if(args.size > 1) args.drop(1).joinToString(" ") else null,
                        duration = seconds,
                        date = Instant.now(),
                        type = GuildPunishment.Type.TEMP_BAN
                )
            }
            else
            {
                throw IllegalArgumentException("Durations must be longer than 60 seconds!")
            }
        }
        else
        {
            throw MissingArgumentException("Please provide the duration")
        }
    }

    override fun getRestAction(target: Member): RestAction<*>? {
        return target.ban(0)
    }
}