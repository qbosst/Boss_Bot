package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.commands.converters.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments

// region: Required (single) converters

fun Arguments.duration(displayName: String) =
    arg(displayName, DurationConverter())

fun Arguments.lengthyString(displayName: String, maxLength: Int) =
    arg(displayName, LengthyStringConverter(maxLength))

// endregion

// region: Optional (single) Converters

fun Arguments.optionalDuration(displayName: String, outputError: Boolean = false) =
    arg(displayName, DurationConverter().toOptional(outputError = outputError))

fun Arguments.optionalLengthyString(displayName: String, maxLength: Int, outputError: Boolean = false) =
    arg(displayName, LengthyStringConverter(maxLength).toOptional(outputError = outputError))

// endregion

// region: Coalescing converters

fun Arguments.coalescingDuration(displayName: String) =
    arg(displayName, DurationConverter().toCoalescing())

fun Arguments.coalescingLengthyString(displayName: String, maxLength: Int) =
    arg(displayName, LengthyStringConverter(maxLength).toCoalescing())

fun Arguments.unionOf(displayName: String, vararg converters: Converter<*>) =
    arg(displayName, UnionConverter(converters.toList()))

// endregion

// region: Optional coalescing converters

fun Arguments.optionalCoalescingDuration(displayName: String) =
    arg(displayName, DurationConverter().toCoalescing().toOptional())

fun Arguments.optionalUnionOf(displayName: String, vararg converters: Converter<*>) =
    arg(displayName, UnionConverter(converters.toList()).toOptional())

// endregion

// region: Converter methods

fun <T: Any> SingleConverter<T>.toCoalescing(
    signatureTypeString: String? = null,
    showTypeInSignature: Boolean? = null,
    errorTypeString: String? = null
): CoalescingConverter<T> = SingleToCoalescingConverter(
    this,
    signatureTypeString,
    showTypeInSignature,
    errorTypeString
)