package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.*
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import kotlinx.datetime.TimeZone
import me.qbosst.bossbot.util.findZones

@Converter(
    "timeZone",
    types = [ConverterType.DEFAULTING, ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE]
)
class TimeZoneConverter(
    override var validator: Validator<TimeZone> = null
): SingleConverter<TimeZone>() {

    override val signatureTypeString: String = "timeZone"

    override suspend fun parse(arg: String, context: CommandContext): Boolean {
        val timeZone: TimeZone = parseTimeZone(arg, context)

        parsed = timeZone
        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }

    private fun parseTimeZone(arg: String, context: CommandContext): TimeZone {
        val timeZones: List<TimeZone> = TimeZone.findZones(arg)

        return when(timeZones.size) {
            0 -> throw CommandException("Could not find any time zones")
            1 -> timeZones.first()
            else -> timeZones.first()
        }
    }
}