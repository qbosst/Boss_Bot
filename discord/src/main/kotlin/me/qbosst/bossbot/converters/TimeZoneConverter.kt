@file:OptIn(
    ConverterToDefaulting::class,
    ConverterToMulti::class,
    ConverterToOptional::class
)

package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.*
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import kotlinx.datetime.TimeZone
import me.qbosst.bossbot.util.findZones

class TimeZoneConverter(
    override var validator: Validator<TimeZone> = null
): SingleConverter<TimeZone>() {

    override val signatureTypeString: String = "timeZone"

    override suspend fun parse(arg: String, context: CommandContext): Boolean {
        val timeZone = parseTimeZone(arg, context)

        parsed = timeZone
        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }

    private fun parseTimeZone(arg: String, context: CommandContext): TimeZone {
        val timeZones = TimeZone.findZones(arg)

        return when(timeZones.size) {
            0 -> throw CommandException("Could not find any time zones")
            1 -> timeZones.first()
            else -> timeZones.first()
        }
    }
}

/**
 * Create a TimeZone converter, for single arguments.
 *
 * @see TimeZoneConverter
 */
public fun Arguments.timeZone(
    displayName: String,
    description: String,
    validator: Validator<TimeZone> = null,
): SingleConverter<TimeZone> =
    arg(displayName, description, TimeZoneConverter(validator))

/**
 * Create an optional TimeZone converter, for single arguments.
 *
 * @see TimeZoneConverter
 */
public fun Arguments.optionalTimeZone(
    displayName: String,
    description: String,
    outputError: Boolean = false,
    validator: Validator<TimeZone?> = null,
): OptionalConverter<TimeZone?> =
    arg(
        displayName,
        description,
        TimeZoneConverter()
            .toOptional(outputError = outputError, nestedValidator = validator)
    )

/**
 * Create a defaulting TimeZone converter, for single arguments.
 *
 * @see TimeZoneConverter
 */
public fun Arguments.defaultingTimeZone(
    displayName: String,
    description: String,
    defaultValue: TimeZone,
    validator: Validator<TimeZone> = null,
): DefaultingConverter<TimeZone> =
    arg(
        displayName,
        description,
        TimeZoneConverter()
            .toDefaulting(defaultValue, nestedValidator = validator)
    )

/**
 * Create a TimeZone converter, for lists of arguments.
 *
 * @param required Whether command parsing should fail if no arguments could be converted.
 *
 * @see TimeZoneConverter
 */
public fun Arguments.timeZoneList(
    displayName: String,
    description: String,
    required: Boolean = true,
    validator: Validator<List<TimeZone>> = null,
): MultiConverter<TimeZone> =
    arg(
        displayName,
        description,
        TimeZoneConverter()
            .toMulti(required, nestedValidator = validator)
    )