package me.qbosst.bossbot.entities.parsers

import me.qbosst.jda.ext.commands.entities.Context
import me.qbosst.jda.ext.commands.parsers.Parser
import me.qbosst.jda.ext.util.TimeUtil
import java.time.ZoneId
import java.util.*

class ZoneIdParser: Parser<ZoneId>
{
    override suspend fun parse(ctx: Context, param: String): Optional<ZoneId> =
        Optional.ofNullable(TimeUtil.filterZones(param).firstOrNull())

    override suspend fun parse(ctx: Context, params: List<String>): Pair<Array<ZoneId>, List<String>> =
        Parser.defaultParse(this, ctx, params)
}