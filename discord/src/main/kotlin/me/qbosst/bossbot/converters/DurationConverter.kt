package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import me.qbosst.bossbot.util.abbreviation
import me.qbosst.bossbot.util.regex
import kotlin.time.Duration
import kotlin.time.DurationUnit

@Converter(
    name = "duration",
    types = [ConverterType.OPTIONAL, ConverterType.DEFAULTING, ConverterType.SINGLE, ConverterType.LIST]
)
class DurationConverter(
    override var validator: Validator<Duration> = null
): SingleConverter<Duration>() {
    override val signatureTypeString: String = "duration"

    override suspend fun parse(arg: String, context: CommandContext): Boolean {
        val parsedTime: Duration = parseTime(arg)
            ?: throw CommandException("That is not a valid duration.")
        this.parsed = parsedTime

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }
}

@Converter(
    name = "duration",
    types = [ConverterType.COALESCING, ConverterType.DEFAULTING, ConverterType.SINGLE, ConverterType.OPTIONAL]
)
class CoalescingDurationConverter(
    override var validator: Validator<Duration> = null
): CoalescingConverter<Duration>() {
    override val signatureTypeString: String = "duration"

    override suspend fun parse(args: List<String>, context: CommandContext): Int {
        val parsedTime: Duration = parseTime(args.joinToString(" "))
            ?: throw CommandException("That is not a valid duration.")
        this.parsed = parsedTime

        return args.size
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }
}

private fun parseTime(time: String): Duration? {
    val units = DurationUnit.values().joinToString("|") { it.regex }
    val timeRegex = "(?is)^((\\s*-?\\s*\\d+\\s*($units)\\s*,?\\s*(and)?)*).*".toRegex()

    var timeStr = time.replace(timeRegex, "$1")
    if(timeStr.isNotBlank()) {
        var totalMillis = 0L

        timeStr = timeStr.replace("(?i)(\\s|,|and)".toRegex(), "")
            .replace("(?is)(-?\\d+|[a-z]+)".toRegex(), "$1 ")
            .trim { it <= ' ' }
        val values = timeStr.split("\\s+".toRegex())

        try {
            var i = 0
            while (i < values.size) {
                val unit: DurationUnit = values[i+1].lowercase()
                    .let { prefix -> DurationUnit.values().first { unit -> prefix.startsWith(unit.abbreviation) } }
                val millis = values[i].toLong()
                    .let { DurationUnit.MILLISECONDS.convert(it, unit) }

                totalMillis += millis
                i += 2
            }
        } catch (e: Exception) {
            return Duration.ZERO
        }

        return Duration.milliseconds(totalMillis)
    } else {
        return null
    }
}