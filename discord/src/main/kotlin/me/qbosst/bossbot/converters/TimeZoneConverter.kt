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
import me.qbosst.bossbot.util.toJavaTimeZone

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
}

private fun parseTimeZone(arg: String, context: CommandContext): TimeZone {
    val timeZones = findZones(arg)

    return when(timeZones.size) {
        0 -> throw CommandException("Could not find any time zones")
        1 -> timeZones.first()
        else -> timeZones.first()
    }
}

private fun findZones(query: String): List<TimeZone> {
    val connector = "[-_\\s/]?"
    val separator = "[^/]+".toPattern()
    val replaceMatching = "[_-]".toRegex()
    val replaceWith = "[-_\\\\s]?"

    val queryLower = query.lowercase()

    return TimeZone.availableZoneIds.asSequence()
        .sorted()
        .map { id ->
            val nameRegex = buildString {
                append('(')

                val matcher = separator.matcher(id)
                while(matcher.find()) {
                    val regex = matcher.group().replace(replaceMatching, replaceWith)
                    append("(${regex})?${connector}")
                }
                deleteRange(lastIndex-connector.length, lastIndex)
                append(')')
            }

            val zoneId = TimeZone.of(id)
            val abbreviationRegex = buildString {
                append('(')
                val timeZone = zoneId.toJavaTimeZone()
                append("${timeZone.getDisplayName(true, 0)}|${timeZone.getDisplayName(false, 0)}")
                append(')')
            }

            zoneId to "($nameRegex)|$abbreviationRegex)".lowercase().toRegex()
        }
        .filter { (_, regex) ->
            queryLower.matches(regex)
        }
        .map { (key, _) ->
            key
        }
        .toList()
}