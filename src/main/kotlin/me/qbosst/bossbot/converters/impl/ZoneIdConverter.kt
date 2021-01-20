package me.qbosst.bossbot.converters.impl

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import me.qbosst.bossbot.util.TimeUtil
import java.time.ZoneId

class ZoneIdConverter: SingleConverter<ZoneId>() {
    override val signatureTypeString: String = "zone id"

    override suspend fun parse(arg: String, context: CommandContext, bot: ExtensibleBot): Boolean {
        val zoneId = TimeUtil.filterZones(arg).firstOrNull()
            ?: throw ParseException("'${arg}' is not a valid zone id")

        this.parsed = zoneId
        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }
}