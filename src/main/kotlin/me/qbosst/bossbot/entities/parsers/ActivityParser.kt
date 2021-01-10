package me.qbosst.bossbot.entities.parsers

import me.qbosst.jda.ext.commands.entities.IContext
import me.qbosst.jda.ext.commands.parsers.Parser
import net.dv8tion.jda.api.entities.Activity
import java.util.*

class ActivityParser: Parser<Activity> {

    override suspend fun parse(ctx: IContext, param: String): Optional<Activity> {
        val params = param.split("[|]".toRegex(), 3).map { it.trim() }

        val type = Activity.ActivityType.values()
            .firstOrNull { it.name.equals(params[0], true) || it.key.toString().equals(param, true) }
            ?: return Optional.empty()

        val name = params.getOrNull(1)
            ?: return Optional.empty()

        val url = params.getOrNull(2)

        return Optional.of(Activity.of(type, name, url))
    }

    override suspend fun parse(ctx: IContext, params: List<String>) = Parser.defaultParse(this, ctx, params)
}