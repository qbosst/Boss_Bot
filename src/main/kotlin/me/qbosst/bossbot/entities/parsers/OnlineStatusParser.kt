package me.qbosst.bossbot.entities.parsers

import me.qbosst.jda.ext.commands.entities.IContext
import me.qbosst.jda.ext.commands.parsers.Parser
import net.dv8tion.jda.api.OnlineStatus
import java.util.*

class OnlineStatusParser: Parser<OnlineStatus> {

    override suspend fun parse(ctx: IContext, param: String): Optional<OnlineStatus> {
        val status = OnlineStatus.values()
            .firstOrNull { it.name.equals(param, true) || it.key.equals(param, true) }

        return Optional.ofNullable(status)
    }

    override suspend fun parse(ctx: IContext, params: List<String>) = Parser.defaultParse(this, ctx, params)
}