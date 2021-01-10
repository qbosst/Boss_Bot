package me.qbosst.bossbot.entities.parsers

import me.qbosst.jda.ext.commands.entities.IContext
import me.qbosst.jda.ext.commands.parsers.Parser
import me.qbosst.jda.ext.util.TimeUtil
import java.time.ZoneId
import java.util.*

class ZoneIdParser: Parser<ZoneId>
{
    override suspend fun parse(ctx: IContext, param: String): Optional<ZoneId> =
        Optional.ofNullable(TimeUtil.filterZones(param).firstOrNull())

    override suspend fun parse(ctx: IContext, params: List<String>): Pair<Array<ZoneId>, List<String>> =
        Parser.defaultParse(this, ctx, params)
}