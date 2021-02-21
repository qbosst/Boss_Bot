package me.qbosst.bossbot.commands.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import dev.kord.rest.builder.interaction.OptionsBuilder

/**
 * Converts a [SingleConverter] to a [CoalescingConverter]. This will consume all the arguments left, so this
 * is intended to only be used when the converter is the last converter in a set of converters
 */
class SingleToCoalescingConverter<T: Any>(
    val singleConverter: SingleConverter<T>,
    newSignatureTypeString: String? = null,
    newShowTypeInSignature: Boolean? = null,
    newErrorTypeString: String? = null,
    override val shouldThrow: Boolean = true
): CoalescingConverter<T>() {

    override val signatureTypeString: String = newSignatureTypeString ?: singleConverter.signatureTypeString
    override val showTypeInSignature: Boolean = newShowTypeInSignature ?: singleConverter.showTypeInSignature
    override val errorTypeString: String? = newErrorTypeString ?: singleConverter.errorTypeString

    override suspend fun parse(args: List<String>, context: CommandContext, bot: ExtensibleBot): Int {
        val arg = args.joinToString(" ")

        try {
            val result = singleConverter.parse(arg, context, bot)

            if(result) {
                this.parsed = singleConverter.parsed
            }

            return args.size
        } catch (e: ParseException) {
            if(shouldThrow) {
                throw e
            }
            return 0
        }
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder = singleConverter.toSlashOption(arg)

    override suspend fun handleError(
        t: Throwable,
        values: List<String>,
        context: CommandContext,
        bot: ExtensibleBot
    ): String = singleConverter.handleError(t, values.joinToString(" "), context, bot)
}