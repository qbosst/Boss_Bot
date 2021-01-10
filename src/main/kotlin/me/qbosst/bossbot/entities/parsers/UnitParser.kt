package me.qbosst.bossbot.entities.parsers

import me.qbosst.jda.ext.commands.entities.IContext
import me.qbosst.jda.ext.commands.parsers.Parser
import java.util.*

/**
 * Unit represents nothing, the absence of input.
 */
class UnitParser: Parser<Unit> {

    override suspend fun parse(ctx: IContext, param: String): Optional<Unit> = when {
        words.contains(param.toLowerCase()) -> Optional.of(Unit)
        else -> Optional.empty()
    }

    override suspend fun parse(ctx: IContext, params: List<String>): Pair<Array<Unit>, List<String>> =
        Parser.defaultParse(this, ctx, params)

    companion object {
        val words = listOf("none", "null", "clear", "default")
    }
}