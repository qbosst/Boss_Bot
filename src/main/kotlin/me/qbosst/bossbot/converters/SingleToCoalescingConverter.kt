package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter

class SingleToCoalescingConverter<T: Any>(
    val singleConverter: SingleConverter<T>,
    newSignatureTypeString: String? = null,
    newShowTypeInSignature: Boolean? = null,
    newErrorTypeString: String? = null
): CoalescingConverter<T>() {

    override val signatureTypeString: String = newSignatureTypeString ?: singleConverter.signatureTypeString
    override val showTypeInSignature: Boolean = newShowTypeInSignature ?: singleConverter.showTypeInSignature
    override val errorTypeString: String? = newErrorTypeString ?: singleConverter.errorTypeString

    override suspend fun parse(args: List<String>, context: CommandContext, bot: ExtensibleBot): Int {
        val arg = args.joinToString(" ")
        val result = singleConverter.parse(arg, context, bot)

        if(result) {
            this.parsed = singleConverter.parsed
        }

        return args.size
    }

    override suspend fun handleError(
        t: Throwable,
        values: List<String>,
        context: CommandContext,
        bot: ExtensibleBot
    ): String = singleConverter.handleError(t, values.joinToString(" "), context, bot)
}