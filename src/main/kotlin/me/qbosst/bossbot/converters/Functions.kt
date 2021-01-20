@file:OptIn(ConverterToOptional::class, ConverterToDefaulting::class, ConverterToMulti::class)

package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import me.qbosst.bossbot.converters.impl.*
import java.awt.Color as Colour
import java.time.Duration
import java.time.ZoneId


// region: Converters

fun <T: Any> SingleConverter<T>.toCoalescing(
    signatureTypeString: String? = null,
    showTypeInSignature: Boolean? = null,
    errorTypeString: String? = null,
    shouldThrow: Boolean = false
) = SingleToCoalescingConverter(this, signatureTypeString, showTypeInSignature, errorTypeString, shouldThrow)

fun Arguments.union(
    displayName: String,
    description: String,
    shouldThrow: Boolean = false,
    vararg converters: Converter<*>,
) = arg(displayName, description, UnionConverter(converters.toList(), shouldThrow))

fun Arguments.optionalUnion(
    displayName: String,
    description: String,
    outputError: Boolean = false,
    vararg converters: Converter<*>,
) = arg(displayName, description, UnionConverter(converters.toList(), shouldThrow = outputError).toOptional(outputError = outputError))

// region: Single Converters

fun Arguments.colour(
    displayName: String,
    description: String,
    colourProvider: suspend (CommandContext) -> Map<String, Colour> = { mapOf() }
) = arg(displayName, description, ColourConverter(colourProvider))

fun Arguments.duration(displayName: String, description: String) =
    arg(displayName, description, DurationConverter())

fun Arguments.maxLengthString(displayName: String, description: String, maxLength: Int) =
    arg(displayName, description, MaxLengthStringConverter(maxLength))

fun Arguments.zoneId(displayName: String, description: String) =
    arg(displayName, description, ZoneIdConverter())

// region: Single Optional Converters

fun Arguments.optionalColour(
    displayName: String,
    description: String,
    colourProvider: suspend (CommandContext) -> Map<String, Colour> = { mapOf() },
    outputError: Boolean = false
) = arg(displayName, description, ColourConverter(colourProvider).toOptional(outputError = outputError))

fun Arguments.optionalDuration(displayName: String, description: String, outputError: Boolean = false) =
    arg(displayName, description, DurationConverter().toOptional(outputError = outputError))

fun Arguments.optionalMaxLengthString(
    displayName: String,
    description: String,
    maxLength: Int,
    outputError: Boolean = false
) = arg(displayName, description, MaxLengthStringConverter(maxLength).toOptional(outputError = outputError))

fun Arguments.optionalZoneId(displayName: String, description: String, outputError: Boolean = false) =
    arg(displayName, description, ZoneIdConverter().toOptional(outputError = outputError))

// region: Single Default Converters

fun Arguments.defaultingColour(
    displayName: String,
    description: String,
    colourProvider: suspend (CommandContext) -> Map<String, Colour> = { mapOf() },
    defaultColour: Colour
) = arg(displayName, description, ColourConverter(colourProvider).toDefaulting(defaultColour))

fun Arguments.defaultingDuration(displayName: String, description: String, defaultValue: Duration) =
    arg(displayName, description, DurationConverter().toDefaulting(defaultValue))

fun Arguments.defaultingMaxLengthString(
    displayName: String,
    description: String,
    maxLength: Int,
    defaultValue: String
) = arg(displayName, description, MaxLengthStringConverter(maxLength).toDefaulting(defaultValue))

fun Arguments.defaultingZoneId(displayName: String, description: String, defaultValue: ZoneId) =
    arg(displayName, description, ZoneIdConverter().toDefaulting(defaultValue))

// region: Single List Converters

fun Arguments.colourList(
    displayName: String,
    description: String,
    colourProvider: suspend (CommandContext) -> Map<String, Colour> = { mapOf() },
    required: Boolean = true
) = arg(displayName, description, ColourConverter(colourProvider).toMulti(required = required))

// region: Coalescing Converters

fun Arguments.coalescedColour(
    displayName: String,
    description: String,
    shouldThrow: Boolean = false,
    colourProvider: suspend (CommandContext) -> Map<String, Colour> = { mapOf() },
) = arg(displayName, description, ColourConverter(colourProvider).toCoalescing(shouldThrow = shouldThrow))

fun Arguments.coalescedZoneId(displayName: String, description: String, shouldThrow: Boolean = false) =
    arg(displayName, description, ZoneIdConverter().toCoalescing(shouldThrow = shouldThrow))

// region: Optional Coalescing Converters

fun Arguments.optionalCoalescedDuration(
    displayName: String,
    description: String,
    outputError: Boolean = false
) = arg(displayName, description, DurationConverter().toCoalescing(shouldThrow = outputError).toOptional(outputError = outputError))


