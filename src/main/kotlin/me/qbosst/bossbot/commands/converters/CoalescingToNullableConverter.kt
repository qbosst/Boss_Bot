package me.qbosst.bossbot.commands.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.OptionalCoalescingConverter

class CoalescingToNullableConverter<T: Any>(
    val coalescingConverter: CoalescingConverter<T>,
): OptionalCoalescingConverter<T>(outputError = true) {

    override val signatureTypeString: String = coalescingConverter.signatureTypeString
    override val showTypeInSignature: Boolean = coalescingConverter.showTypeInSignature

    override suspend fun parse(args: List<String>, context: CommandContext, bot: ExtensibleBot): Int {
        val arg = args.first()
        if(SET_WORDS.any { word -> arg.equals(word, true) }) {
            this.parsed = null
            return 1
        } else {
            val result = coalescingConverter.parse(args, context, bot)
            this.parsed = coalescingConverter.parsed
            return result
        }
    }

    companion object {
        val SET_WORDS = listOf("null", "none", "default")
    }
}